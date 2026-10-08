package com.example.bodega.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class CustomerEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String phone;
    private String dni;
    private double totalDebt;
    private String notes;

    public CustomerEntity(String name, String phone, String dni, double totalDebt, String notes) {
        this.name = name;
        this.phone = phone;
        this.dni = dni;
        this.totalDebt = totalDebt;
        this.notes = notes;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public double getTotalDebt() { return totalDebt; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
