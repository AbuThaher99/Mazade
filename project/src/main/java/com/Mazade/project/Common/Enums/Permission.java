package com.Mazade.project.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    ADMIN_READ("Admin:read"),
    ADMIN_UPDATE("Admin:update"),
    ADMIN_CREATE("Admin:create"),
    ADMIN_DELETE("Admin:delete"),
    DOCTOR_READ("Doctor:read"),
    DOCTOR_UPDATE("Doctor:update"),
    NURSE_READ("Nurse:read"),
    NURSE_UPDATE("Nurse:update"),
    SECRETARY_READ("Secretary:read"),
    SECRETARY_UPDATE("Secretary:update"),
    DRUG_STORE_EMPLOYEE_READ("DrugStoreEmployee:read"),
    DRUG_STORE_EMPLOYEE_UPDATE("DrugStoreEmployee:update"),
    PATIENT_READ("Patient:read"),
    PATIENT_UPDATE("Patient:update");

    private final String permission;
}
