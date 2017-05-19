package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class PendingTransferListAdapter extends AbstractEditableListAdapter
{
	public static final String FIELD_TRANSFER_TOTAL_COUNT = "pseudoTotalCount";

	private Transaction mTransaction;
	private ArrayList<CursorItem> mList = new ArrayList<>();
	private SQLQuery.Select mSelect;

	public PendingTransferListAdapter(Context context)
	{
		super(context);
		mTransaction = new Transaction(context);
		mSelect = new SQLQuery.Select(Transaction.TABLE_TRANSFER)
				.setOrderBy(Transaction.FIELD_TRANSFER_ID + " DESC")
				.setGroupBy(MainDatabase.FIELD_TRANSFER_GROUPID)
				.setLoadListener(new SQLQuery.Select.LoadListener()
				{
					@Override
					public void onOpen(SQLiteDatabase db, Cursor cursor)
					{

					}

					@Override
					public void onLoad(SQLiteDatabase db, Cursor cursor, CursorItem item)
					{
						ArrayList<CursorItem> itemList = mTransaction.getTable(new SQLQuery.Select(Transaction.TABLE_TRANSFER)
								.setWhere(Transaction.FIELD_TRANSFER_GROUPID + "=?", item.getString(Transaction.FIELD_TRANSFER_GROUPID)));

						item.putAll(itemList.get(0)); // First item will be loaded first so better show it
						item.put(FIELD_TRANSFER_TOTAL_COUNT, itemList.size());
					}
				});
	}

	public PendingTransferListAdapter(Context context, int groupId)
	{
		this(context);
		mSelect = new SQLQuery.Select(Transaction.TABLE_TRANSFER)
				.setWhere(Transaction.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId));
	}

	public PendingTransferListAdapter(Context context, String deviceId)
	{
		this(context);
		mSelect = new SQLQuery.Select(Transaction.TABLE_TRANSFER)
				.setWhere(Transaction.FIELD_TRANSFER_DEVICEID + "=?", deviceId);
	}

	@Override
	protected void onSearch(String word)
	{

	}

	@Override
	protected void onUpdate()
	{
		mList.clear();
		mList.addAll(mTransaction.getTable(mSelect));
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int i)
	{
		return mList.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_pending_transfer, viewGroup, false);

		final CursorItem thisItem = (CursorItem) getItem(i);
		final boolean isIncoming = thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING;
		final boolean isGroup = thisItem.exists(FIELD_TRANSFER_TOTAL_COUNT);

		ImageView typeImage = (ImageView) view.findViewById(R.id.list_process_type_image);
		ImageView clearImage = (ImageView) view.findViewById(R.id.list_process_clear_image);
		TextView mainText = (TextView) view.findViewById(R.id.list_process_name_text);
		TextView statusText = (TextView) view.findViewById(R.id.list_process_status_text);
		TextView countText = (TextView) view.findViewById(R.id.list_process_count_text);

		typeImage.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
		mainText.setText(thisItem.getString(MainDatabase.FIELD_TRANSFER_NAME));
		statusText.setText(thisItem.getString(MainDatabase.FIELD_TRANSFER_FLAG));
		countText.setText((thisItem.exists(FIELD_TRANSFER_TOTAL_COUNT) && thisItem.getInt(FIELD_TRANSFER_TOTAL_COUNT) > 1) ? "+" + (thisItem.getInt(FIELD_TRANSFER_TOTAL_COUNT) - 1) : "");

		clearImage.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

				dialog.setTitle(R.string.dialog_title_remove_queue_job);
				dialog.setMessage(isGroup ?
						mContext.getString(R.string.dialog_msg_remove_queue_job, thisItem.getInt(FIELD_TRANSFER_TOTAL_COUNT)) :
						mContext.getString(R.string.warning_remove_pending_transfer, thisItem.getString(Transaction.FIELD_TRANSFER_NAME)));

				dialog.setNegativeButton(R.string.close, null);
				dialog.setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (isGroup)
							mTransaction.removeTransactionGroup(thisItem.getInt(Transaction.FIELD_TRANSFER_GROUPID));
						else
							mTransaction.removeTransaction(thisItem.getInt(Transaction.FIELD_TRANSFER_ID));
					}
				}).show();
			}
		});

		return view;
	}
}