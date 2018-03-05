<%
    // connect
    javax.naming.InitialContext ctx = 
            new javax.naming.InitialContext();
    javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup(
            "java:/comp/env/" + "jdbc/fz");

    try (java.sql.Connection con = ds.getConnection()){
        String sql = "delete from fbAIRun";
        try (java.sql.PreparedStatement ps = con.prepareStatement(sql)){
            ps.executeUpdate();
        }
    }
%>
OK