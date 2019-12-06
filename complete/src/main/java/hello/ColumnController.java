package hello;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.influxdb.dto.QueryResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import org.apache.iotdb.jdbc.IoTDBSQLException;

@RestController
public class ColumnController {
    @RequestMapping("/columns")
    public List<Column> columns(
            @RequestParam(value="url", defaultValue = "jdbc:iotdb://127.0.0.1:6667/") String url,
            @RequestParam(value="username", defaultValue = "root") String username,
            @RequestParam(value="password", defaultValue = "root") String password,
            @RequestParam(value="database", defaultValue="root") String database,
            @RequestParam(value="timeseries", defaultValue="root") String timeseries,
            @RequestParam(value="ip", required = false) String ip,
            @RequestParam(value="port", required = false) String port,
            @RequestParam(value="dbtype", defaultValue = "iotdb") String dbtype
    ) throws SQLException {

        url = url.replace("\"", "");
        username = username.replace("\"", "");
        password = password.replace("\"", "");
        database = database.replace("\"", "");
        timeseries = timeseries.replace("\"", "");
        dbtype = dbtype.replace("\"", "");
        ip = ip == null ? null : ip.replace("\"", "");
        port = port == null ? null : port.replace("\"", "");

        List<Column> columns = new LinkedList<>();

        if(dbtype.toLowerCase().equals("iotdb")){
            if(ip != null && port != null) url = String.format("jdbc:iotdb://%s:%s/", ip, port);

            Connection connection = IoTDBConnection.getConnection(url, username, password);
            if (connection == null) {
                System.out.println("get connection defeat");
                return null;
            }
            Statement statement = connection.createStatement();
            String sql = "SHOW TIMESERIES " +
                    database.replace("\"", "") + "." +
                    timeseries.replace("\"", "");
            statement.execute(sql);
            ResultSet resultSet = statement.getResultSet();
            HashSet<String> set = new HashSet<>();
            if (resultSet != null) {
                final ResultSetMetaData metaData = resultSet.getMetaData();
                final int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    String column = resultSet.getString(1).split("\\.")[3];
                    String type = resultSet.getString(3);
                    String encoding = resultSet.getString(4);
                    if(!set.contains(column)){
                        columns.add(new Column(column, type, encoding));
                        set.add(column);
                    }
                }
            }
            statement.close();
            connection.close();
            return columns;
        }
        else if(dbtype.toLowerCase().equals("pg")){
            return null;
        }
        else if(dbtype.toLowerCase().equals("influxdb")){
            if(ip != null && port != null) url = String.format("http://%s:%s/", ip, port);

            InfluxDBConnection influxDBConnection = new InfluxDBConnection(url, username, password, database, null);
            QueryResult res = influxDBConnection.query("show field keys");
            for(QueryResult.Result r : res.getResults()){
                for(QueryResult.Series s : r.getSeries()){
                    String name = s.getName();
                    System.out.println(name);
                    if(!name.equals(timeseries)) continue;
                    for(List<Object> x :s.getValues()){
                        columns.add(new Column(x.get(0).toString(), x.get(1).toString(), null));
                    }
                }
            }
            influxDBConnection.close();
            return columns;
        }
        else return null;
    }
}
