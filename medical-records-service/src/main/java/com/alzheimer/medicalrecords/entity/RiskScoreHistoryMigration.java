package com.alzheimer.medicalrecords.entity;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import java.sql.*;

public class RiskScoreHistoryMigration
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url      = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password", "");
        if (url == null || username == null) return;

        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { return; }

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt  = conn.createStatement()) {

            fixRiskScoreHistoryTable(conn, stmt);
            ensureMedicalRecordColumns(conn, stmt);

        } catch (Exception e) {
            System.err.println("[DB-Migration] WARNING: " + e.getMessage());
        }
    }

    private void fixRiskScoreHistoryTable(Connection conn, Statement stmt) throws Exception {
        if (!tableExists(conn, "risk_score_history")) return;
        boolean hasOld = columnExists(conn, "risk_score_history", "record_id");
        boolean hasNew = columnExists(conn, "risk_score_history", "medical_record_id");
        if (hasOld && !hasNew) {
            stmt.execute("ALTER TABLE risk_score_history CHANGE COLUMN record_id medical_record_id BIGINT NOT NULL");
        } else if (!hasOld && !hasNew) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("DROP TABLE IF EXISTS risk_score_history");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            return;
        }
        addColumnIfMissing(conn, stmt, "risk_score_history", "wellness_contribution",
                "ALTER TABLE risk_score_history ADD COLUMN wellness_contribution DOUBLE");
        addColumnIfMissing(conn, stmt, "risk_score_history", "hereditary_contribution",
                "ALTER TABLE risk_score_history ADD COLUMN hereditary_contribution DOUBLE");
        addColumnIfMissing(conn, stmt, "risk_score_history", "trigger_reason",
                "ALTER TABLE risk_score_history ADD COLUMN trigger_reason VARCHAR(50)");
    }

    private void ensureMedicalRecordColumns(Connection conn, Statement stmt) {
        if (!tableExists(conn, "medical_records")) return;
        addColumnIfMissing(conn, stmt, "medical_records", "hereditary_risk_contribution",
                "ALTER TABLE medical_records ADD COLUMN hereditary_risk_contribution DOUBLE DEFAULT 0.0");
        addColumnIfMissing(conn, stmt, "medical_records", "wellness_risk_contribution",
                "ALTER TABLE medical_records ADD COLUMN wellness_risk_contribution DOUBLE DEFAULT 0.0");
        addColumnIfMissing(conn, stmt, "medical_records", "apoe_status",
                "ALTER TABLE medical_records ADD COLUMN apoe_status VARCHAR(20) DEFAULT \'NOT_TESTED\'");
        addColumnIfMissing(conn, stmt, "medical_records", "diagnosis_stage",
                "ALTER TABLE medical_records ADD COLUMN diagnosis_stage VARCHAR(20) DEFAULT \'PRECLINICAL\'");
        addColumnIfMissing(conn, stmt, "medical_records", "stage_manually_overridden",
                "ALTER TABLE medical_records ADD COLUMN stage_manually_overridden BIT(1) DEFAULT 0");
    }

    private boolean tableExists(Connection conn, String table) {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
            return rs.next();
        } catch (Exception e) { return false; }
    }

    private boolean columnExists(Connection conn, String table, String column) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        } catch (Exception e) { return false; }
    }

    private void addColumnIfMissing(Connection conn, Statement stmt, String table, String column, String sql) {
        if (!columnExists(conn, table, column)) {
            try { stmt.execute(sql); System.out.println("[DB-Migration] Added " + column + " to " + table); }
            catch (Exception e) { System.err.println("[DB-Migration] Could not add " + column + ": " + e.getMessage()); }
        }
    }
}
