package com.dyrnq.distops;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeDir {
    private String homeAbsolutePath;
    private String tmpAbsolutePath;
}
