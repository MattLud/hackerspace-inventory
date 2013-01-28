package org.atxhackerspace.inventory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class InventoryActivity extends Activity {
	IntentIntegrator integrator;
	
	EditText username, password, item_code, item_name, item_description;
	ImageView image_to_upload;
	
	public static final int SHRINK_MAX = 1024;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 42;
	
	private WikiWhack ww;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inventory);

		ww = new WikiWhack(this.getApplicationContext(), "atxhackerspace.org");
		
		integrator = new IntentIntegrator(this);

		username = (EditText) findViewById(R.id.wiki_username);
		password = (EditText) findViewById(R.id.wiki_password);
		item_code = (EditText) findViewById(R.id.item_code);
		image_to_upload = (ImageView) findViewById(R.id.image_to_upload);
		item_name = (EditText) findViewById(R.id.item_name);
		item_description = (EditText) findViewById(R.id.item_description);
		
		setImagePreview();

		final Button scan = (Button) findViewById(R.id.scan);
		scan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				integrator.initiateScan();
			}
		});

		final Button take_picture = (Button) findViewById(R.id.take_picture);
		take_picture.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				Log.d("Onclick", Uri.fromFile(getTempFile()).toString());
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile()));
				startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
			}
		});
		
		final Button submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					InventoryItem toPost = new InventoryItem(username.getText().toString(), password.getText().toString(),
							item_code.getText().toString(), getTempFile(true), getPreviewImage(),
							item_name.getText().toString(), item_description.getText().toString());
					if (toPost.username.matches("") || toPost.password.matches("") || toPost.item_code.matches("") || toPost.item_name.matches("") || toPost.item_description.matches("")) {
						Toast.makeText(InventoryActivity.this, "All fields are required!", Toast.LENGTH_LONG).show();
						return;
					}
					ww.create(toPost);
					
				} catch (FileNotFoundException e) {
					Toast.makeText(InventoryActivity.this, "Take a picture to upload first!", Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	public void setImagePreview() {
		try {
			Bitmap bm = getPreviewImage();
			image_to_upload.setImageBitmap(bm);
		}
		// It's fine if there's no file.
		catch (FileNotFoundException e) {
			Log.d("setImagePreview", "File Not Found");
		}
	}
	
	public Bitmap getPreviewImage() throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(getTempFile());
		Bitmap imageBitmap = BitmapFactory.decodeStream(fis);

		Float width  = Float.valueOf(imageBitmap.getWidth());
		Float height = Float.valueOf(imageBitmap.getHeight());
		
		Integer new_width, new_height;
		if (width > height) {
			Float ratio = height / width;
			new_width = SHRINK_MAX;
			new_height = (int)(ratio * SHRINK_MAX);
		} else {
			Float ratio = width / height;
			new_width = (int)(ratio * SHRINK_MAX);
			new_height = SHRINK_MAX;
		}
		Log.d("getPreviewImage", String.format("Shrinking from %d, %d to %d, %d", width.intValue(), height.intValue(), new_width, new_height));
		imageBitmap = Bitmap.createScaledBitmap(imageBitmap, new_width, new_height, false);
		FileOutputStream fos = new FileOutputStream(getTempFile(true));
		imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
		return imageBitmap;
	}
	
	private File getTempFile(Boolean shrunk) {
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		if (!path.exists()) path.mkdir();
		if (shrunk) {
			return new File(path, "last-inventory-pic-small.jpg");
		} else {
			return new File(path, "last-inventory-pic.jpg");
		}
	}
	
	private File getTempFile() {
		return getTempFile(false);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				setImagePreview();
			}
		} else {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(
					requestCode, resultCode, intent);
			if (scanResult != null) {
				String[] url_parts = scanResult.getContents().split("/");
				String code = url_parts[url_parts.length - 1];
				item_code.setText(code);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_inventory, menu);
		return true;
	}

}
