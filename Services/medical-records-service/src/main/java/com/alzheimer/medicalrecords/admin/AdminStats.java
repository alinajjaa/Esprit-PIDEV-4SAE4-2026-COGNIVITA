package com.alzheimer.medicalrecords.admin;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;
import com.alzheimer.medicalrecords.mmse.*;

public class AdminStats {
    private long usersCount;
    private long mmseTestsCount;
    private long activeUsersCount;

    public AdminStats(long usersCount, long mmseTestsCount, long activeUsersCount) {
        this.usersCount = usersCount;
        this.mmseTestsCount = mmseTestsCount;
        this.activeUsersCount = activeUsersCount;
    }

    public long getUsersCount() { return usersCount; }
    public void setUsersCount(long usersCount) { this.usersCount = usersCount; }

    public long getMmseTestsCount() { return mmseTestsCount; }
    public void setMmseTestsCount(long mmseTestsCount) { this.mmseTestsCount = mmseTestsCount; }

    public long getActiveUsersCount() { return activeUsersCount; }
    public void setActiveUsersCount(long activeUsersCount) { this.activeUsersCount = activeUsersCount; }
}
