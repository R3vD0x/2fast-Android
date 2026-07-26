package br.com.itisoft.a2fast.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import br.com.itisoft.a2fast.App;
import br.com.itisoft.a2fast.R;
import br.com.itisoft.a2fast.data.DataSession;
import br.com.itisoft.a2fast.model.DatafileEntry;
import br.com.itisoft.a2fast.model.TwoFACodeModel;

public class AccountListActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AccountAdapter adapter;
    private TextView emptyView;
    private boolean hideCodes;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            DataSession.get().refreshCodes();
            adapter.submit(DataSession.get().getAccounts(), hideCodes);
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);

        if (!DataSession.get().isLoaded()) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }

        DatafileEntry active = App.get().preferences().getActiveDatafile();
        if (getSupportActionBar() != null && active != null && active.displayName != null) {
            getSupportActionBar().setTitle(active.displayName);
        }

        hideCodes = App.get().preferences().useHiddenTotp();
        emptyView = findViewById(R.id.textEmpty);
        RecyclerView list = findViewById(R.id.recyclerAccounts);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        adapter = new AccountAdapter(new AccountAdapter.Listener() {
            @Override
            public void onCopy(TwoFACodeModel model) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("totp", model.TwoFACode));
                Toast.makeText(AccountListActivity.this, R.string.code_copied, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(TwoFACodeModel model) {
                confirmDelete(model);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        fab.setOnClickListener(v -> startActivity(new Intent(this, AddAccountActivity.class)));
        refreshEmptyState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!DataSession.get().isLoaded()) {
            return;
        }
        hideCodes = App.get().preferences().useHiddenTotp();
        DataSession.get().refreshCodes();
        adapter.submit(DataSession.get().getAccounts(), hideCodes);
        refreshEmptyState();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accounts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_hide) {
            hideCodes = !hideCodes;
            App.get().preferences().setUseHiddenTotp(hideCodes);
            adapter.submit(DataSession.get().getAccounts(), hideCodes);
            return true;
        }
        if (id == R.id.action_manage_files) {
            Intent intent = new Intent(this, DataFilesActivity.class);
            intent.putExtra(DataFilesActivity.EXTRA_MANAGE_ONLY, true);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_lock) {
            DataSession.get().lock();
            Intent intent = new Intent(this, DataFilesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete(TwoFACodeModel model) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(getString(R.string.delete_account_message, model.Label))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    DataSession.get().removeAccount(model);
                    persist();
                    adapter.submit(DataSession.get().getAccounts(), hideCodes);
                    refreshEmptyState();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void persist() {
        try {
            DatafileEntry active = App.get().preferences().getActiveDatafile();
            if (active == null || active.uri == null) {
                Toast.makeText(this, R.string.error_write_datafile, Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = Uri.parse(active.uri);
            String json = DataSession.get().serializeCurrent();
            App.get().datafileStorage().write(uri, json);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_write_datafile, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshEmptyState() {
        boolean empty = DataSession.get().getAccounts().isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    static final class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.Holder> {

        interface Listener {
            void onCopy(TwoFACodeModel model);

            void onDelete(TwoFACodeModel model);
        }

        private final Listener listener;
        private final List<TwoFACodeModel> items = new ArrayList<>();
        private boolean hideCodes;

        AccountAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<TwoFACodeModel> accounts, boolean hide) {
            items.clear();
            items.addAll(accounts);
            hideCodes = hide;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            TwoFACodeModel model = items.get(position);
            String title = model.Label;
            if (model.Issuer != null && !model.Issuer.isEmpty()
                    && !model.Issuer.equalsIgnoreCase(model.Label)) {
                title = model.Issuer + " · " + model.Label;
            }
            holder.title.setText(title);
            String code = model.TwoFACode == null ? "" : model.TwoFACode;
            if (hideCodes && code.length() >= 2 && !"Error".equals(code)) {
                holder.code.setText(code.replaceAll(".", "•"));
            } else if (code.length() == 6) {
                holder.code.setText(code.substring(0, 3) + " " + code.substring(3));
            } else {
                holder.code.setText(code);
            }
            int max = Math.max(model.Period, 1);
            int remaining = (int) Math.max(0, Math.min(max, model.Seconds));
            holder.progress.setMax(max);
            holder.progress.setProgress(remaining);
            holder.seconds.setText(holder.itemView.getContext()
                    .getString(R.string.seconds_remaining, remaining));
            holder.itemView.setOnClickListener(v -> listener.onCopy(model));
            holder.itemView.setOnLongClickListener(v -> {
                listener.onDelete(model);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView code;
            final TextView seconds;
            final ProgressBar progress;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textTitle);
                code = itemView.findViewById(R.id.textCode);
                seconds = itemView.findViewById(R.id.textSeconds);
                progress = itemView.findViewById(R.id.progressSeconds);
            }
        }
    }
}
