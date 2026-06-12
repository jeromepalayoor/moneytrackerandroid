package net.jpalayoor.moneytracker.ui.home;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import net.jpalayoor.moneytracker.data.AppDatabase;
import net.jpalayoor.moneytracker.data.Transaction;
import net.jpalayoor.moneytracker.data.TransactionDao;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    private final ExecutorService executor;
    public LiveData<List<Transaction>> transactions;
    public LiveData<Double> monthlyInflow;
    public LiveData<Double> monthlyOutflow;

    public HomeViewModel(Application application) {
        super(application);

        // initialise database
        transactionDao = AppDatabase.getInstance(application).transactionDao();
        executor = Executors.newSingleThreadExecutor();

        // get current month
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
                .format(new Date());

        // get transactions, monthly inflow and outflow data from database
        transactions = transactionDao.getTransactionsByMonth(currentMonth);
        monthlyInflow = transactionDao.getMonthlyInflow(currentMonth);
        monthlyOutflow = transactionDao.getMonthlyOutflow(currentMonth);
    }

    // insert new transaction
    public void insert(Transaction transaction) {
        executor.execute(() -> transactionDao.insert(transaction));
    }

    // delete transaction
    public void delete(Transaction transaction) {
        executor.execute(() -> transactionDao.delete(transaction));
    }

    // update transaction details
    public void update(Transaction transaction) {
        executor.execute(() -> transactionDao.update(transaction));
    }
}