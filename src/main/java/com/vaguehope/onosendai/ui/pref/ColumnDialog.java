package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.NotificationStyle;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.ui.pref.ColumnChooser.ColumnChoiceListener;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.DialogHelper.Question;
import com.vaguehope.onosendai.util.EqualHelper;

class ColumnDialog {

	private static final List<Duration> REFRESH_DURAITONS = CollectionHelper.listOf(
			new Duration(0, "Never"),
			new Duration(15, "15 minutes"),
			new Duration(30, "30 minutes"),
			new Duration(45, "45 minutes"),
			new Duration(60, "1 hour"),
			new Duration(60 * 2, "2 hours"),
			new Duration(60 * 3, "3 hours"),
			new Duration(60 * 6, "6 hours"),
			new Duration(60 * 12, "12 hours"),
			new Duration(60 * 24, "24 hours")
			);

	private final Context context;
	private final Map<Integer, Column> allColumns;

	private final int id;
	private final String accountId;
	private final Column initialValue;
	private final Account account;
	private final Prefs prefs;

	private final View llParent;
	private final EditText txtTitle;
	private final Spinner spnPosition;
	private final Button btnResource;
	private final Button btnExclude;
	private final Spinner spnRefresh;
	private final CheckBox chkInlineMedia;
	private final CheckBox chkHdMedia;
	private final CheckBox chkNotify;
	private final CheckBox chkDelete;
	private final TableRow rowNotification;
	private final Button btnNotification;

	private String resource;
	private final Set<Integer> excludes = new HashSet<Integer>();
	private NotificationStyle notificationStyle = null;

	public ColumnDialog (final Context context, final Prefs prefs, final int id, final String accountId) {
		this(context, prefs, id, accountId, null);
	}

	public ColumnDialog (final Context context, final Prefs prefs, final Column initialValue) {
		this(context, prefs, // NOSONAR Sonar things there is a Possible null pointer dereference.  I disagree.
		initialValue != null ? initialValue.getId() : null,
				initialValue != null ? initialValue.getAccountId() : null,
				initialValue);
	}

	private ColumnDialog (final Context context, final Prefs prefs, final int id, final String accountId, final Column initialValue) {
		this.context = context;

		if (prefs == null) throw new IllegalArgumentException("Prefs can not be null.");
		if (initialValue != null && initialValue.getId() != id) throw new IllegalStateException("ID and initialValue ID do not match.");
		if (initialValue != null && !EqualHelper.equal(initialValue.getAccountId(), accountId)) throw new IllegalStateException("Account ID and initialValue account ID do not match.");

		this.id = id;
		this.accountId = accountId;
		this.initialValue = initialValue;
		this.prefs = prefs;

		try {
			this.allColumns = Collections.unmodifiableMap(prefs.readColumnsAsMap());
			this.account = prefs.readAccount(accountId);
		}
		catch (final JSONException e) {
			throw new IllegalStateException(e); // FIXME
		}

		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.columndialog, null);

		this.txtTitle = (EditText) this.llParent.findViewById(R.id.txtTitle);
		this.spnPosition = (Spinner) this.llParent.findViewById(R.id.spnPosition);
		this.btnResource = (Button) this.llParent.findViewById(R.id.btnResource);
		this.btnExclude = (Button) this.llParent.findViewById(R.id.btnExclude);
		this.spnRefresh = (Spinner) this.llParent.findViewById(R.id.spnRefresh);
		this.chkInlineMedia = (CheckBox) this.llParent.findViewById(R.id.chkInlineMedia);
		this.chkHdMedia = (CheckBox) this.llParent.findViewById(R.id.chkHdMedia);
		this.chkNotify = (CheckBox) this.llParent.findViewById(R.id.chkNotify);
		this.chkDelete = (CheckBox) this.llParent.findViewById(R.id.chkDelete);
		this.rowNotification = (TableRow) this.llParent.findViewById(R.id.rowNotification);
		this.btnNotification = (Button) this.llParent.findViewById(R.id.btnNotification);

		final ArrayAdapter<Integer> posAdapter = new ArrayAdapter<Integer>(context, R.layout.numberspinneritem);
		posAdapter.addAll(CollectionHelper.sequence(1, prefs.readColumnIds().size() + (initialValue == null ? 1 : 0)));
		this.spnPosition.setAdapter(posAdapter);

		this.btnResource.setOnClickListener(this.btnResourceClickListener);
		this.btnResource.setOnLongClickListener(this.btnResourceLongClickListener);
		this.btnExclude.setOnClickListener(this.btnExcludeClickListener);
		this.chkNotify.setOnClickListener(this.chkNotifyClickListener);
		this.btnNotification.setOnClickListener(this.btnNotificationClickListener);

		final ArrayAdapter<Duration> refAdapter = new ArrayAdapter<Duration>(context, R.layout.numberspinneritem);
		refAdapter.addAll(REFRESH_DURAITONS);
		this.spnRefresh.setAdapter(refAdapter);

		this.chkDelete.setChecked(false);

		if (initialValue != null) {
			this.txtTitle.setText(initialValue.getTitle());
			this.spnPosition.setSelection(posAdapter.getPosition(Integer.valueOf(prefs.readColumnPosition(initialValue.getId()) + 1)));
			setResource(initialValue.getResource());
			setExcludeIds(initialValue.getExcludeColumnIds());
			setDurationSpinner(initialValue.getRefreshIntervalMins(), refAdapter);
			if (this.account == null) this.spnRefresh.setEnabled(false);
			this.chkInlineMedia.setChecked(initialValue.isInlineMedia());
			this.chkHdMedia.setChecked(initialValue.isHdMedia());
			setNotificationStyle(initialValue.getNotificationStyle());
			this.chkDelete.setVisibility(View.VISIBLE);
		}
		else {
			this.spnPosition.setSelection(posAdapter.getCount() - 1); // Last item.
			setDurationSpinner(0, refAdapter); // Default to no background refresh.
			this.chkDelete.setVisibility(View.GONE);
		}
	}

	protected void setExcludeColumns (final Set<Column> excludes) {
		final Set<Integer> ids = new HashSet<Integer>();
		for (final Column column : excludes) {
			ids.add(Integer.valueOf(column.getId()));
		}
		setExcludeIds(ids);
	}

	public void setResource (final String resource) {
		this.resource = resource;
		this.btnResource.setText(String.valueOf(resource).replace(':', '\n'));
		final AccountProvider provider = this.account != null ? this.account.getProvider() : null;
		this.btnResource.setEnabled(provider == AccountProvider.TWITTER || provider == AccountProvider.SUCCESSWHALE);
	}

	private void setExcludeIds (final Set<Integer> excludes) {
		this.excludes.clear();
		if (excludes != null) this.excludes.addAll(excludes);
		redrawExcludes();
	}

	private void redrawExcludes () {
		final StringBuilder s = new StringBuilder();
		if (this.excludes.size() > 0) {
			for (final Integer exId : this.excludes) {
				if (s.length() > 0) s.append(", ");
				final Column exCol = this.allColumns.get(exId);
				s.append(exCol != null ? exCol.getUiTitle() : "(" + exId + ")");
			}
		}
		else {
			s.append("(none)");
		}
		this.btnExclude.setText(s.toString());
	}

	protected void setNotificationStyle (final NotificationStyle ns) {
		this.notificationStyle = ns;
		this.chkNotify.setChecked(ns != null);
		redrawNotificationStyle();
	}

	protected void updateNotificationStyle () {
		if (this.chkNotify.isChecked()) {
			if (this.notificationStyle == null) this.notificationStyle = NotificationStyle.DEFAULT;
		}
		else {
			this.notificationStyle = null;
		}
		redrawNotificationStyle();
	}

	private void redrawNotificationStyle () {
		this.rowNotification.setVisibility(this.notificationStyle != null ? View.VISIBLE : View.GONE);
		this.btnNotification.setText(this.notificationStyle != null ? this.notificationStyle.getUiTitle() : "false");
	}

	private final OnClickListener btnResourceClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			btnResourceClick();
		}
	};

	private final OnLongClickListener btnResourceLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick (final View v) {
			btnResourceLongClick();
			return true;
		}
	};

	private final OnClickListener btnExcludeClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			btnExcludeClick();
		}
	};

	private final OnClickListener chkNotifyClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			updateNotificationStyle();
		}
	};

	private final OnClickListener btnNotificationClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			btnNotificationClick();
		}
	};

	protected void btnResourceClick () {
		new ColumnChooser(this.context, this.prefs, new ColumnChoiceListener() {
			@Override
			public void onColumn (final Account unused, final String newResource, final String title) {
				if (newResource != null) setResource(newResource);
				if (title != null) setTitle(title);
			}
		}).promptAddColumn(this.account); // TODO pass in existing resource value.
	}

	protected void btnResourceLongClick () {
		DialogHelper.askString(this.context, "Resource:", this.resource, true, false, new Listener<String>() {
			@Override
			public void onAnswer (final String newResource) {
				if (newResource != null) setResource(newResource);
			}
		});
	}

	protected void btnExcludeClick () {
		final List<Column> columns = new ArrayList<Column>(this.allColumns.values());
		final Iterator<Column> iterator = columns.iterator();
		while (iterator.hasNext()) {
			if (this.id == iterator.next().getId()) iterator.remove();
		}

		final Set<Integer> exes = this.excludes;
		DialogHelper.askItems(this.context, "Exclude items also in:", columns, new Question<Column>() {
			@Override
			public boolean ask (final Column arg) {
				return exes.contains(Integer.valueOf(arg.getId()));
			}
		}, new Listener<Set<Column>>() {
			@Override
			public void onAnswer (final Set<Column> answer) {
				setExcludeColumns(answer);
			}
		});
	}

	protected void btnNotificationClick () {
		final NotificationStyleDialog dlg = new NotificationStyleDialog(this.context, this.notificationStyle);
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this.context);
		dlgBuilder.setTitle(dlg.getUiTitle());
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				setNotificationStyle(dlg.getValue());
				dialog.dismiss();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});
		dlgBuilder.create().show();
	}

	private void setDurationSpinner (final int mins, final ArrayAdapter<Duration> refAdapter) {
		final Duration duration = new Duration(mins > 0 ? mins : 0);
		final int pos = refAdapter.getPosition(duration);
		if (pos < 0) refAdapter.add(duration); // Allow for any value to have been chosen before.
		this.spnRefresh.setSelection(pos < 0 ? refAdapter.getPosition(duration) : pos);
	}

	public Column getInitialValue () {
		return this.initialValue;
	}

	public View getRootView () {
		return this.llParent;
	}

	public void setTitle (final String title) {
		this.txtTitle.setText(title);
	}

	/**
	 * 0 based.
	 */
	public int getPosition () {
		return ((Integer) this.spnPosition.getSelectedItem()) - 1;
	}

	public boolean isDeleteSelected () {
		return this.chkDelete.isChecked();
	}

	public Column getValue () {
		return new Column(this.id,
				this.txtTitle.getText().toString(),
				this.accountId,
				this.resource,
				((Duration) this.spnRefresh.getSelectedItem()).getMins(),
				this.excludes.size() > 0 ? this.excludes : null,
				this.notificationStyle,
				this.chkInlineMedia.isChecked(),
				this.chkHdMedia.isChecked());
	}

	private static class Duration {

		private final int mins;
		private final String title;

		public Duration (final int mins) {
			this(mins, null);
		}

		public Duration (final int mins, final String title) {
			this.mins = mins;
			this.title = title;
		}

		public int getMins () {
			return this.mins;
		}

		@Override
		public String toString () {
			return this.title;
		}

		@Override
		public boolean equals (final Object o) {
			if (o == null) return false;
			if (o == this) return true;
			if (!(o instanceof Duration)) return false;
			final Duration that = (Duration) o;
			return this.mins == that.mins;
		}

		@Override
		public int hashCode () {
			return this.mins;
		}

	}

}
