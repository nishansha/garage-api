package com.triasoft.garage.constants;

public enum ExpenseLockWindow {
    IMMEDIATE, // Lock as soon as the sale is recorded — no edits allowed from sale date onwards
    EOD,       // End of sale day
    EOM,       // End of sale month
    EOQ,       // End of calendar quarter (Jan-Mar, Apr-Jun, Jul-Sep, Oct-Dec) in which sale occurred
    EOY        // End of calendar year (Dec 31) in which sale occurred
}
