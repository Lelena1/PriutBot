package com.example.priutbot.entity;

public enum ClientRole {
    /** Anyone who has not yet been registered as a new owner by a volunteer. */
    GUEST,
    /** Adopted an animal and is in (or has completed) the daily-report / probation flow. */
    NEW_OWNER,
    /** Staff member; can use volunteer-only commands. */
    VOLUNTEER
}
