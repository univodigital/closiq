package com.closiq.seller.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SellerProductBulkImportServiceTest {

    @Test
    void csvParserHandlesQuotedFields() throws Exception {
        Method parse = SellerProductBulkImportService.CsvParser.class.getDeclaredMethod("parse", String.class);
        parse.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String[]> rows = (List<String[]>) parse.invoke(null, "title,description\n\"Sherwani\",\"A long, detailed description\"");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)[0]).isEqualTo("Sherwani");
        assertThat(rows.get(1)[1]).isEqualTo("A long, detailed description");
    }
}
