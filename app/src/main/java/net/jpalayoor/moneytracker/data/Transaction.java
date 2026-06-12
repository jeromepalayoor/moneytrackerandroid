package net.jpalayoor.moneytracker.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// creating transactions table
@Entity(tableName = "transactions")
public class Transaction {
    // auto create primary key
    @PrimaryKey(autoGenerate = true)
    public int id;

    // column names
    public String name;
    public String category;
    public double amount;
    public String date;
    public String note;
}