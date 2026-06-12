package net.jpalayoor.moneytracker.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Update
    void update(Transaction transaction);

    // get transaction data of a certain month and order by date descending
    @Query("SELECT * FROM transactions WHERE date LIKE :month || '%' ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByMonth(String month);

    // get the total sum of inflow transactions of a certain month
    @Query("SELECT SUM(amount) FROM transactions WHERE amount > 0 AND date LIKE :month || '%'")
    LiveData<Double> getMonthlyInflow(String month);

    // get the total sum of outflow transactions of a certain month
    @Query("SELECT SUM(amount) FROM transactions WHERE amount < 0 AND date LIKE :month || '%'")
    LiveData<Double> getMonthlyOutflow(String month);

    // get distinct months that have transactions formatted as "yyyy-mm"
    @Query("SELECT DISTINCT substr(date, 1, 7) as month FROM transactions ORDER BY month DESC")
    LiveData<List<String>> getDistinctMonths();

    // get total sum of inflow for every month
    @Query("SELECT substr(date, 1, 7) as month, SUM(amount) as total FROM transactions WHERE amount > 0 GROUP BY month ORDER BY month DESC")
    LiveData<List<MonthTotal>> getMonthlyInflowTotals();

    // get total sum of outflow for every month
    @Query("SELECT substr(date, 1, 7) as month, SUM(amount) as total FROM transactions WHERE amount < 0 GROUP BY month ORDER BY month DESC")
    LiveData<List<MonthTotal>> getMonthlyOutflowTotals();

    // get all transaction data order by date descending for export
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<Transaction> getAllTransactionsSync();
}