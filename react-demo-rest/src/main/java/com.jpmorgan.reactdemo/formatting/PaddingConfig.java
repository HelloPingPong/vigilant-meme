package com.jpmorgan.reactdemo.formatting;

import com.jpmorgan.reactdemo.formatting.enums.PaddingPosition;
import lombok.Data;

@Data
public class PaddingConfig {
    private Character character = ' '; // Default padding character
    private PaddingPosition position = PaddingPosition.RIGHT; // Default position
}
