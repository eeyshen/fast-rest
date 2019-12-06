package hello;

import java.util.LinkedList;
import java.util.List;

import org.influxdb.dto.QueryResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import org.apache.iotdb.jdbc.IoTDBSQLException;

@RestController
public class StorageGroupController {
    @RequestMapping("/database")
    public List<StorageGroup> storageGroup(
            @RequestParam(value="url", defaultValue = "jdbc:iotdb://127.0.0.1:6667/") String url,
            @RequestParam(value="username", defaultValue = "root") String username,
            @RequestParam(value="password", defaultValue = "root") String password,
            @RequestParam(value="ip", required = false) String ip,
            @RequestParam(value="port", required = false) String port,
            @RequestParam(value="dbtype", defaultValue = "iotdb") String dbtype
    ) throws SQLException {

        url = url.replace("\"", "");
        username = username.replace("\"", "");
        password = password.replace("\"", "");
        dbtype = dbtype.replace("\"", "");
        ip = ip == null ? null : ip.replace("\"", "");
        port = port == null ? null : port.replace("\"", "");

        List<StorageGroup> storageGroup = new LinkedList<>();

        if(dbtype.toLowerCase().equals("iotdb")){
            if(ip != null && port != null) url = String.format("jdbc:iotdb://%s:%s/", ip, port);
            Connection connection = IoTDBConnection.getConnection(url, username, password);
            if (connection == null) {
                System.out.println("get connection defeat");
                return null;
            }
            Statement statement = connection.createStatement();
            String sql = "SHOW STORAGE GROUP";
            statement.execute(sql);
            ResultSet resultSet = statement.getResultSet();

            if (resultSet != null) {
                final ResultSetMetaData metaData = resultSet.getMetaData();
                final int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    storageGroup.add(new StorageGroup(resultSet.getString(1)));
                }
            }
            statement.close();
            connection.close();
            return storageGroup;
        }
        else if(dbtype.toLowerCase().equals("pg")){
            if(ip != null && port != null) url = String.format("jdbc:postgresql://%s:%s/", ip, port);
            PGConnection pgtool = new PGConnection(url, username, password);
            Connection connection = pgtool.getConn();
            if (connection == null) {
                System.out.println("get connection defeat");
                return null;
            }
            String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
            ResultSet resultSet = pgtool.query(connection, sql);
            while(resultSet.next()){
                storageGroup.add(new StorageGroup(resultSet.getString(1)));
            }
            connection.close();
            return storageGroup;
        }
        else if(dbtype.toLowerCase().equals("influxdb")){
            if(ip != null && port != null) url = String.format("http://%s:%s/", ip, port);
            InfluxDBConnection influxDBConnection = new InfluxDBConnection(url, username, password, null, null);
            QueryResult res = influxDBConnection.query("show databases");
            for(QueryResult.Result r : res.getResults()){
                for(QueryResult.Series s : r.getSeries()){
                    for(List<Object> x :s.getValues()){
                        for(Object o : x){
                            storageGroup.add(new StorageGroup(o.toString()));
                        }
                    }
                }
            }
            return storageGroup;
        }
        else return null;


    }
}
