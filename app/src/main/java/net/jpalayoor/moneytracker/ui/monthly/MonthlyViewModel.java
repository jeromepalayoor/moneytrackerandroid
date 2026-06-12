package net.jpalayoor.moneytracker.ui.monthly;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import net.jpalayoor.moneytracker.data.AppDatabase;
import net.jpalayoor.moneytracker.data.MonthTotal;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.data.TransactionDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonthlyViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    public LiveData<List<String>> distinctMonths;
    public LiveData<List<MonthTotal>> monthlyInflowTotals;
    public LiveData<List<MonthTotal>> monthlyOutflowTotals;

    public MonthlyViewModel(Application application) {
        super(application);
        transactionDao = AppDatabase.getInstance(application).transactionDao();
        distinctMonths = transactionDao.getDistinctMonths();
        monthlyInflowTotals = transactionDao.getMonthlyInflowTotals();
        monthlyOutflowTotals = transactionDao.getMonthlyOutflowTotals();
    }

    public LiveData<List<Transaction>> getTransactionsByMonth(String month) {
        return transactionDao.getTransactionsByMonth(month);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void update(Transaction transaction) {
        executor.execute(() -> transactionDao.update(transaction));
    }

    public void delete(Transaction transaction) {
        executor.execute(() -> transactionDao.delete(transaction));
    }
}