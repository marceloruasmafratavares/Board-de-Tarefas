package dio.board;

import dio.board.persistence.migration.MigrationStrategy;
import dio.board.ui.MainMenu;

import java.sql.SQLException;

import static dio.board.persistence.config.ConnectionConfig.getConnection;


public class Main {

    public static void main(String[] args) throws SQLException {
        try(var connection = getConnection()){
            new MigrationStrategy(connection).executeMigration();
        }
        new MainMenu().execute();
    }

}