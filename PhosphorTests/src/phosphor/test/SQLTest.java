/*
Crude test for SQL
Server creates taint
Uploads value into DB if taint is present
Variable is retainted @ clientside
 */
package phosphor.test;

import java.sql.*;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class SQLTest1 {
    // JDBC driver name, DB url, and credentials
    /*
    Specific test steps necessary:
    1. Create a DB (MySQL) called EMP with user/pass: root/password
    2. Create a table called Test_DB
    3. Create columns called id, age, first, last, and taint

    Prog. func.:
    1. Create a variable taint
    2. Taint entries by checking for taint on original variable, adding taint to a column called taint
    3. In client side, check for value in taint column. Depending on the value, create a client side var
    that is either tainted or not
    4. Check for taint on client side
    */
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/EMP";
    static final String USER = "root";
    static final String PASS = "password";

    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        try{
            //Driver setup
            Class.forName("com.mysql.jdbc.Driver");
            //New connection
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
            //SQL Query code:
            stmt = conn.createStatement();
            int idOne = Tainter.taintedInt(1);
            int idTwo = 2;
            if(Tainter.getTaint(idOne.intValue() == 0)
                stmt.execute("INSERT INTO Test_DB (id, age, first, last) VALUES (1, 20, 'John', 'Smith', 0)");
            else
                stmt.execute("INSERT INTO Test_DB (id, age, first, last) VALUES (1, 20, 'John', 'Smith', 1)");
            String sql;
            sql = "SELECT * FROM Test_DB";
            ResultSet rs = stmt.executeQuery(sql);
            int id = rs.getInt("id");
            int age = rs.getInt("age");
            String first = rs.getString("first");
            String last = rs.getString("last");
            int taint = rs.getInt("taint");
            if(taint != 0)
                int idCopy = Tainter.taintedInt(1);
            else
                int idCopy = 0;

            //Check here:
            assert(Tainter.getTaint(idCopy.intValue() != 0)));


            //Clear table and exit
            stmt.execute("DELETE FROM Test_DB");
            rs.close();
            stmt.close();
            conn.close();
        } catch(SQLException se) {
            //JDBC Errors
            se.printStackTrace();
        } catch(Exception e) {
            //Class.forName errors
            e.printStackTrace();
        } finally{
            try {
                if(stmt!=null)
                    stmt.close();
            } catch(SQLException se2){/*Leave empty*/}
            try {
                if(conn!=null)
                    conn.close();
            } catch(SQLException se) {
                se.printStackTrace();
            }
        }
    }
}