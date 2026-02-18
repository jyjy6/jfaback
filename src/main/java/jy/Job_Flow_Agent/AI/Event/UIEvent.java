package jy.Job_Flow_Agent.AI.Event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UIEvent {
    private String type;
    private Object data;
}
