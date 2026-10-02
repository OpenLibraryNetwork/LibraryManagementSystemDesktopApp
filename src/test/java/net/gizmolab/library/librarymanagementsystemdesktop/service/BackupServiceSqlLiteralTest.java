package net.gizmolab.library.librarymanagementsystemdesktop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The backup paths go into H2's SCRIPT/RUNSCRIPT as string literals, so a quote in a folder name must not end the literal. */
class BackupServiceSqlLiteralTest {

    @Test
    void aPlainPathIsQuoted() {
        assertEquals("'/home/user/backup/dump.sql'", BackupService.sqlLiteral("/home/user/backup/dump.sql"));
    }

    @Test
    void aQuoteInThePathIsDoubled() {
        assertEquals("'/home/o''brien/dump.sql'", BackupService.sqlLiteral("/home/o'brien/dump.sql"));
    }
}
