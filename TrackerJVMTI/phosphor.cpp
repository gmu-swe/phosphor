#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include "jvmti.h"
#include "jni.h"
#define _STRING(s) #s
#define STRING(s) _STRING(s)
typedef struct {
	/* JVMTI Environment */
	jvmtiEnv *jvmti;
	JNIEnv * jni;
	jboolean vm_is_started;
	jboolean vmDead;

	/* Data access Lock */
	jrawMonitorID lock;
	JavaVM* jvm;
} GlobalAgentData;
static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities capa;
static GlobalAgentData *gdata;
/* Send message to stdout or whatever the data output location is */
void stdout_message(const char * format, ...) {
	va_list ap;

	va_start(ap, format);
	(void) vfprintf(stdout, format, ap);
	va_end(ap);
}
/* Send message to stderr or whatever the error output location is and exit  */
void fatal_error(const char * format, ...) {
	va_list ap;

	va_start(ap, format);
	(void) vfprintf(stderr, format, ap);
	(void) fflush(stderr);
	va_end(ap);
	exit(3);
}

/* Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
static void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum,
		const char *str) {
	if (errnum != JVMTI_ERROR_NONE) {
		char *errnum_str;

		errnum_str = NULL;
		(void) jvmti->GetErrorName(errnum, &errnum_str);

		printf("ERROR: JVMTI: %d(%s): %s\n", errnum,
				(errnum_str == NULL ? "Unknown" : errnum_str),
				(str == NULL ? "" : str));
	}
}

/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
static void enter_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;

	error = jvmti->RawMonitorEnter(gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
static void exit_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;

	error = jvmti->RawMonitorExit(gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}

void describe(jvmtiError err) {
	jvmtiError err0;
	char *descr;
	err0 = jvmti->GetErrorName(err, &descr);
	if (err0 == JVMTI_ERROR_NONE) {
		printf(descr);
	} else {
		printf("error [%d]", err);
	}
}

JNIEXPORT static void JNICALL setObjExpression(JNIEnv *env, jclass klass,
		jobject o, jobject expr) {
	if (gdata->vmDead) {
		return;
	}
	if(!o)
	{
		return;
	}
	jvmtiError error;
	if (expr) {
		error = gdata->jvmti->SetTag(o,	(jlong) (ptrdiff_t) (void*) env->NewGlobalRef(expr));
	} else {
		error = gdata->jvmti->SetTag(o, 0);
	}
	check_jvmti_error(gdata->jvmti, error, "Cannot set object tag");

}
JNIEXPORT static jobject JNICALL getObjExpression(JNIEnv *env, jclass klass,
		jobject o) {
	if (gdata->vmDead) {
		return NULL;
	}
	jvmtiError error;
	jlong tag;
	error = gdata->jvmti->GetTag(o, &tag);
	check_jvmti_error(gdata->jvmti, error, "Cannot get object tag");
	if(tag)
	{
		return (jobject) (ptrdiff_t) tag;
	}
	return NULL;
}
/* Callback for JVMTI_EVENT_VM_START */
static void JNICALL
cbVMStart(jvmtiEnv *jvmti, JNIEnv *env) {

	enter_critical_section(jvmti);
	{
		jclass klass;
		jfieldID field;
		jint rc;

		/* Java Native Methods for class */
		static JNINativeMethod registry[2] = { { "_setTags",
				"(Ljava/lang/Object;[I)V",
				(void*) &setObjExpression }, { "_getTags",
				"(Ljava/lang/Object;)[I",
				(void*) &getObjExpression } };
		/* Register Natives for class whose methods we use */
		klass = env->FindClass("edu/columbia/cs/psl/phosphor/runtime/ArrayHelper");
		if (klass == NULL) {
			fatal_error(
					"ERROR: JNI: Cannot find edu.columbia.cs.psl.knarr.runtime.ArrayHelper with FindClass\n");
		}
		rc = env->RegisterNatives(klass, registry, 2);
		if (rc != 0) {
			fatal_error(
					"ERROR: JNI: Cannot register natives for class edu.columbia.cs.psl.knarr.runtime.ArrayHelper\n");
		}
		/* Engage calls. */
		field = env->GetStaticFieldID(klass, "engaged", "I");
		if (field == NULL) {
			fatal_error("ERROR: JNI: Cannot get field from %s\n",
					STRING(HEAP_TRACKER_class));
		}
		env->SetStaticIntField(klass, field, 1);

		/* Indicate VM has started */
		gdata->vm_is_started = JNI_TRUE;

	}
	exit_critical_section(jvmti);
}
// VM Death callback
static void JNICALL callbackVMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {
	gdata->vmDead = JNI_TRUE;
}
static void JNICALL
cbObjectFree(jvmtiEnv *jvmti_env, jlong tag) {

}
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options,
		void *reserved) {
	static GlobalAgentData data;
	jvmtiError error;
	jint res;
	jvmtiEventCallbacks callbacks;

	/* Setup initial global agent data area
	 *   Use of static/extern data should be handled carefully here.
	 *   We need to make sure that we are able to cleanup after ourselves
	 *     so anything allocated in this library needs to be freed in
	 *     the Agent_OnUnload() function.
	 */
	(void) memset((void*) &data, 0, sizeof(data));
	gdata = &data;

	/*  We need to first get the jvmtiEnv* or JVMTI environment */

	gdata->jvm = jvm;
	res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_0);

	if (res != JNI_OK || jvmti == NULL) {
		/* This means that the VM was unable to obtain this version of the
		 *   JVMTI interface, this is a fatal error.
		 */
		printf("ERROR: Unable to access JVMTI Version 1 (0x%x),"
				" is your J2SE a 1.5 or newer version?"
				" JNIEnv's GetEnv() returned %d\n", JVMTI_VERSION_1, res);

	}

	/* Here we save the jvmtiEnv* for Agent_OnUnload(). */
	gdata->jvmti = jvmti;
	/* Parse any options supplied on java command line */
//    parse_agent_options(options);
	(void) memset(&capa, 0, sizeof(jvmtiCapabilities));
	capa.can_get_line_numbers = 1;
	capa.can_signal_thread = 1;
	capa.can_get_owned_monitor_info = 1;
//	capa.can_generate_method_entry_events = 1;
//	capa.can_generate_exception_events = 1;
//	capa.can_generate_vm_object_alloc_events = 1;
	capa.can_generate_object_free_events = 1;
	capa.can_tag_objects = 1;

	error = jvmti->AddCapabilities(&capa);
	check_jvmti_error(jvmti, error,
			"Unable to get necessary JVMTI capabilities.");

	(void) memset(&callbacks, 0, sizeof(callbacks));
//	callbacks.VMInit = &callbackVMInit; /* JVMTI_EVENT_VM_INIT */
	callbacks.VMDeath = &callbackVMDeath; /* JVMTI_EVENT_VM_DEATH */
	callbacks.VMStart = &cbVMStart;
	callbacks.ObjectFree = &cbObjectFree;

//	callbacks.DataDumpRequest = &dataDumpRequest;

	error = jvmti->SetEventCallbacks(&callbacks, (jint) sizeof(callbacks));
	check_jvmti_error(jvmti, error, "Cannot set jvmti callbacks");

	/* At first the only initial events we are interested in are VM
	 *   initialization, VM death, and Class File Loads.
	 *   Once the VM is initialized we will request more events.
	 */
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START,
			(jthread) NULL);
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT,
			(jthread) NULL);
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH,
			(jthread) NULL);
//	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
//			JVMTI_EVENT_FIELD_ACCESS, (jthread) NULL);
//	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
//			JVMTI_EVENT_FIELD_MODIFICATION, (jthread) NULL);
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
			JVMTI_EVENT_OBJECT_FREE, (jthread) NULL);
//	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
//			JVMTI_EVENT_CLASS_LOAD, (jthread) NULL);
//	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
//			    JVMTI_EVENT_DATA_DUMP_REQUEST, NULL);
//    error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, (jthread)NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");

	/* Here we create a raw monitor for our use in this agent to
	 *   protect critical sections of code.
	 */
	error = jvmti->CreateRawMonitor("agent data", &(gdata->lock));
	check_jvmti_error(jvmti, error, "Cannot create raw monitor");

	/* We return JNI_OK to signify success */
	return JNI_OK;
}
