package com.alzheimer.medicalrecords.admin;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;
import com.alzheimer.medicalrecords.mmse.*;

public class ChartData {
    private String label;
    private long value;
    private String color;

    public ChartData(String label, long value, String color) {
        this.label = label;
        this.value = value;
        this.color = color;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
