package com.triasoft.garage.model.home;

import com.triasoft.garage.dto.ActivityDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class ActivityRs implements Serializable {
    @Serial
    private static final long serialVersionUID = -2808668042183982201L;
    private List<ActivityDTO> activities;
}
