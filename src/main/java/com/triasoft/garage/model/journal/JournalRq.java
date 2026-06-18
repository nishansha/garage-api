package com.triasoft.garage.model.journal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JournalRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate journalDate;
    private String description;
    private List<JournalLineRq> lines;

}
