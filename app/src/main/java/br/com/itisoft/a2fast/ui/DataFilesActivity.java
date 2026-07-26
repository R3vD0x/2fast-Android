package br.com.itisoft.a2fast.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * Lists registered {@code .2fa} files and lets the user open, add, or remove them.
 */
public class DataFilesActivity extends AppCompatActivity {

    public static final String EXTRA_MANAGE_ONLY = "manage_only";

    private DatafileAdapter adapter;
    private TextView emptyView;
    private boolean manageOnly;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datafiles);

        manageOnly = getIntent().getBooleanExtra(EXTRA_MANAGE_ONLY, false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.datafiles_title);
            if (manageOnly) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        emptyView = findViewById(R.id.textEmptyFiles);
        RecyclerView list = findViewById(R.id.recyclerDatafiles);
        FloatingActionButton fab = findViewById(R.id.fabAddDatafile);

        adapter = new DatafileAdapter(new DatafileAdapter.Listener() {
            @Override
            public void onOpen(DatafileEntry entry) {
                openFile(entry);
            }

            @Override
            public void onRemove(DatafileEntry entry) {
                confirmRemove(entry);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        fab.setOnClickListener(v -> showAddOptions());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
        if (!manageOnly && !App.get().preferences().hasConfiguredDatafile()) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_datafiles, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_create) {
            startActivity(new Intent(this, CreateDataFileActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_add_open) {
            startActivity(new Intent(this, OpenDataFileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void refreshList() {
        List<DatafileEntry> files = App.get().preferences().getDatafiles();
        String activeId = App.get().preferences().getActiveDatafileId();
        adapter.submit(files, activeId);
        emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddOptions() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_datafile_title)
                .setItems(new CharSequence[]{
                        getString(R.string.create_datafile),
                        getString(R.string.open_datafile)
                }, (d, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, CreateDataFileActivity.class));
                    } else {
                        startActivity(new Intent(this, OpenDataFileActivity.class));
                    }
                })
                .show();
    }

    private void openFile(DatafileEntry entry) {
        App.get().preferences().setActiveDatafileId(entry.id);
        if (DataSession.get().isLoaded()
                && entry.id.equals(DataSession.get().getActiveFileId())) {
            startActivity(new Intent(this, AccountListActivity.class));
            return;
        }
        DataSession.get().lock();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_FILE_ID, entry.id);
        startActivity(intent);
    }

    private void confirmRemove(DatafileEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_datafile_title)
                .setMessage(getString(R.string.remove_datafile_message, entry.displayName))
                .setPositiveButton(R.string.remove_from_app, (d, w) -> {
                    boolean wasActive = entry.id.equals(DataSession.get().getActiveFileId());
                    App.get().passwordVault().removePasswordForFile(entry.id, entry.passwordHash);
                    App.get().preferences().removeDatafile(entry.id);
                    if (wasActive) {
                        DataSession.get().lock();
                    }
                    refreshList();
                    Toast.makeText(this, R.string.datafile_removed, Toast.LENGTH_SHORT).show();
                    if (!App.get().preferences().hasConfiguredDatafile() && !manageOnly) {
                        Intent intent = new Intent(this, WelcomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static final class DatafileAdapter extends RecyclerView.Adapter<DatafileAdapter.Holder> {

        interface Listener {
            void onOpen(DatafileEntry entry);

            void onRemove(DatafileEntry entry);
        }

        private final Listener listener;
        private final List<DatafileEntry> items = new ArrayList<>();
        private String activeId;

        DatafileAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<DatafileEntry> entries, String activeId) {
            items.clear();
            items.addAll(entries);
            this.activeId = activeId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_datafile, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            DatafileEntry entry = items.get(position);
            holder.title.setText(entry.displayName);
            holder.subtitle.setText(entry.uri);
            boolean active = entry.id != null && entry.id.equals(activeId);
            holder.badge.setVisibility(active ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onOpen(entry));
            holder.itemView.setOnLongClickListener(v -> {
                listener.onRemove(entry);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView subtitle;
            final TextView badge;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textFileName);
                subtitle = itemView.findViewById(R.id.textFileUri);
                badge = itemView.findViewById(R.id.textActiveBadge);
            }
        }
    }
}
