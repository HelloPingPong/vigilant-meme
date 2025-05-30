package com.jpmorgan.reactdemo.formatting;

import com.jpmorgan.reactdemo.formatting.enums.TruncatePosition;
import lombok.Data;

@Data
public class FixedLengthConfig {
    private int length;
    private TruncatePosition truncateFrom = TruncatePosition.END;
    private PaddingConfig padding = new PaddingConfig();
}
