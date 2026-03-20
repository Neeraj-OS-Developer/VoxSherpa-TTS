package com.CodeBySonu.VoxSherpa;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.CodeBySonu.VoxSherpa.databinding.*;
import com.k2fsa.sherpa.onnx.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class LibraryFragmentActivity extends Fragment {
	
	private LibraryFragmentBinding binding;
	ArrayList<HashMap<String, Object>> allList;
	boolean isFavTab = false;
	MediaPlayer mediaPlayer = null;
	int currentlyPlayingPos = -1;
	RecyclerView.Adapter adapter;
	java.util.ArrayList<java.util.HashMap<String, Object>> displayList = new java.util.ArrayList<>();
	
	private SharedPreferences sp2;
	private SharedPreferences sp1;
	private SharedPreferences sp3;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		binding = LibraryFragmentBinding.inflate(_inflater, _container, false);
		initialize(_savedInstanceState, binding.getRoot());
		initializeLogic();
		return binding.getRoot();
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		sp2 = getContext().getSharedPreferences("sp2", Activity.MODE_PRIVATE);
		sp1 = getContext().getSharedPreferences("sp1", Activity.MODE_PRIVATE);
		sp3 = getContext().getSharedPreferences("sp3", Activity.MODE_PRIVATE);
	}
	
	private void initializeLogic() {
		// 1. Setup SharedPreferences & Layout
		binding.recyclerviewHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
		
		// 🚀 FIX 1: Item Blink Animation band karne ke liye (Blink Issue Solution)
		androidx.recyclerview.widget.RecyclerView.ItemAnimator animator = binding.recyclerviewHistory.getItemAnimator();
		if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
			((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
		}
		
		// 2. TABS SWITCHING LOGIC (Inline)
		android.view.View.OnClickListener tabListener = new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				if (v.getId() == binding.textview6.getId() || v.getId() == binding.cardview4.getId()) {
					// "All Recordings" Clicked
					isFavTab = false;
					binding.cardview4.setCardBackgroundColor(android.graphics.Color.parseColor("#2D3748"));
					binding.textview6.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
					binding.textview7.setBackgroundColor(android.graphics.Color.TRANSPARENT);
					binding.textview7.setTextColor(android.graphics.Color.parseColor("#718096"));
				} else if (v.getId() == binding.textview7.getId()) {
					// "Favorites" Clicked
					isFavTab = true;
					binding.cardview4.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
					binding.textview6.setTextColor(android.graphics.Color.parseColor("#718096"));
					binding.textview7.setBackgroundColor(android.graphics.Color.parseColor("#2D3748"));
					binding.textview7.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
				}
				
				// Reload List based on Tab
				displayList.clear();
				if (isFavTab) {
					for (java.util.HashMap<String, Object> item : allList) {
						boolean favCheck = item.containsKey("is_favorite") && item.get("is_favorite") != null && String.valueOf(item.get("is_favorite")).equals("true");
						if (favCheck) {
							displayList.add(item);
						}
					}
				} else {
					displayList.addAll(allList);
				}
				
				// Handle Empty State
				if (displayList.isEmpty()) {
					binding.recyclerviewHistory.setVisibility(android.view.View.GONE);
					binding.linear4.setVisibility(android.view.View.VISIBLE);
					binding.historyStatusTv.setText(isFavTab ? "No favorites yet" : "No history found");
				} else {
					binding.recyclerviewHistory.setVisibility(android.view.View.VISIBLE);
					binding.linear4.setVisibility(android.view.View.GONE);
				}
				
				if (adapter != null) adapter.notifyDataSetChanged();
			}
		};
		
		binding.textview6.setOnClickListener(tabListener);
		binding.cardview4.setOnClickListener(tabListener);
		binding.textview7.setOnClickListener(tabListener);
		
		// 3. INITIAL DATA LOAD FROM SP2 & DEFAULT TAB SELECTION
		String libraryData = sp2.getString("library_list", "[]");
		allList = new com.google.gson.Gson().fromJson(libraryData, new com.google.gson.reflect.TypeToken<java.util.ArrayList<java.util.HashMap<String, Object>>>(){}.getType());
		if (allList == null) allList = new java.util.ArrayList<>();
		
		// 🚀 FIX: Manually trigger the "All Recordings" tab click on startup
		// Ye ensure karega ki UI highlight ho aur list sahi data dikhaye default state mein.
		tabListener.onClick(binding.cardview4); 
		
		// 4. RECYCLERVIEW ADAPTER (Fixed Logic)
		adapter = new androidx.recyclerview.widget.RecyclerView.Adapter() {
			@androidx.annotation.NonNull
			@Override
			public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
				return new androidx.recyclerview.widget.RecyclerView.ViewHolder(getActivity().getLayoutInflater().inflate(R.layout.item_history, parent, false)) {};
			}
			
			@Override
			public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
				android.view.View v = holder.itemView;
				
				// Data fetch using holder position to ensure accuracy
				java.util.HashMap<String, Object> item = displayList.get(holder.getAdapterPosition());
				
				// Set Titles & Voice Name
				((android.widget.TextView) v.findViewById(R.id.txt_title)).setText(item.containsKey("text") ? item.get("text").toString() : "Unknown");
				((android.widget.TextView) v.findViewById(R.id.txt_voice_name)).setText(item.containsKey("voice_name") ? item.get("voice_name").toString() : "Unknown Voice");
				
				// Format Duration & Date
				String dur = item.containsKey("duration") ? item.get("duration").toString() : "0:00";
				String dateStr = "Unknown Date";
				if (item.containsKey("timestamp")) {
					try {
						long millis = Long.parseLong(item.get("timestamp").toString());
						java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US);
						dateStr = sdf.format(new java.util.Date(millis));
					} catch (Exception e){}
				}
				((android.widget.TextView) v.findViewById(R.id.txt_meta)).setText(dur + " • " + dateStr);
				
				// Favorite Icon Setup
				android.widget.ImageView favBtn = v.findViewById(R.id.btn_favorite);
				boolean isFav = item.containsKey("is_favorite") && item.get("is_favorite") != null && String.valueOf(item.get("is_favorite")).equals("true");
				
				if (isFav) {
					favBtn.setImageResource(R.drawable.icon_favorite); 
					favBtn.setColorFilter(android.graphics.Color.parseColor("#FF4B4B")); 
				} else {
					favBtn.setImageResource(R.drawable.icon_favorite_outline);
					favBtn.setColorFilter(android.graphics.Color.parseColor("#A0AEC0")); 
				}
				
				// Play/Pause Icon Setup
				android.widget.ImageView playIcon = v.findViewById(R.id.img_play_pause);
				if (currentlyPlayingPos == holder.getAdapterPosition() && mediaPlayer != null && mediaPlayer.isPlaying()) {
					playIcon.setImageResource(R.drawable.icon_pause_circle);
				} else {
					playIcon.setImageResource(R.drawable.icon_play_circle);
				}
				
				// 1. Favorite Click (Fixed Stale Position)
				favBtn.setOnClickListener(view -> {
					int currentPos = holder.getAdapterPosition();
					if (currentPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return;
					
					java.util.HashMap<String, Object> currentItem = displayList.get(currentPos);
					boolean currentFavState = currentItem.containsKey("is_favorite") && currentItem.get("is_favorite") != null && String.valueOf(currentItem.get("is_favorite")).equals("true");
					boolean newFavState = !currentFavState;
					currentItem.put("is_favorite", String.valueOf(newFavState));
					
					// Sync with main list
					for(int i=0; i<allList.size(); i++){
						if(allList.get(i).get("timestamp").equals(currentItem.get("timestamp"))){
							allList.get(i).put("is_favorite", String.valueOf(newFavState));
							break;
						}
					}
					
					sp2.edit().putString("library_list", new com.google.gson.Gson().toJson(allList)).apply();
					
					if (isFavTab && !newFavState) {
						displayList.remove(currentPos);
						notifyItemRemoved(currentPos);
						if(displayList.isEmpty()){
							binding.recyclerviewHistory.setVisibility(android.view.View.GONE);
							binding.linear4.setVisibility(android.view.View.VISIBLE);
							binding.historyStatusTv.setText("No favorites yet");
						}
					} else {
						notifyItemChanged(currentPos);
					}
				});
				
				// 2. Play Audio Click (Fixed Stale Position)
				v.findViewById(R.id.btn_play_item).setOnClickListener(view -> {
					int currentPos = holder.getAdapterPosition();
					if (currentPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return;
					
					java.util.HashMap<String, Object> currentItem = displayList.get(currentPos);
					String path = currentItem.containsKey("path") ? currentItem.get("path").toString() : "";
					
					if(path.isEmpty()){
						com.google.android.material.snackbar.Snackbar.make(v, "File not found", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
						return;
					}
					
					try {
						if (currentlyPlayingPos == currentPos) {
							if (mediaPlayer != null && mediaPlayer.isPlaying()) {
								mediaPlayer.pause();
								notifyItemChanged(currentPos);
							} else if (mediaPlayer != null) {
								mediaPlayer.start();
								notifyItemChanged(currentPos);
							}
						} else {
							if (mediaPlayer != null) {
								mediaPlayer.release();
							}
							mediaPlayer = new android.media.MediaPlayer();
							mediaPlayer.setDataSource(path);
							mediaPlayer.prepare();
							mediaPlayer.start();
							
							int oldPos = currentlyPlayingPos;
							currentlyPlayingPos = currentPos;
							
							if(oldPos != -1) notifyItemChanged(oldPos);
							notifyItemChanged(currentlyPlayingPos);
							
							// 🚀 FIX 2: Audio completion icon logic using global currentlyPlayingPos
							mediaPlayer.setOnCompletionListener(mp -> {
								int compPos = currentlyPlayingPos; // Save the playing position
								currentlyPlayingPos = -1; // Reset state
								if(compPos != -1) {
									notifyItemChanged(compPos); // Update the exact row
								}
							});
						}
					} catch (Exception e) {
						com.google.android.material.snackbar.Snackbar.make(v, "Error playing file", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
					}
				});
				
				// 3. Share Click
				v.findViewById(R.id.btn_share).setOnClickListener(view -> {
					int currentPos = holder.getAdapterPosition();
					if (currentPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return;
					
					java.util.HashMap<String, Object> currentItem = displayList.get(currentPos);
					String path = currentItem.containsKey("path") ? currentItem.get("path").toString() : "";
					if(!path.isEmpty()){
						try {
							java.io.File file = new java.io.File(path);
							android.os.StrictMode.VmPolicy.Builder builder = new android.os.StrictMode.VmPolicy.Builder();
							android.os.StrictMode.setVmPolicy(builder.build());
							
							android.content.Intent intentShare = new android.content.Intent(android.content.Intent.ACTION_SEND);
							intentShare.setType("audio/wav");
							intentShare.putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(file));
							startActivity(android.content.Intent.createChooser(intentShare, "Share Audio"));
						} catch(Exception e){
							com.google.android.material.snackbar.Snackbar.make(v, "Error sharing file", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
						}
					}
				});
				
				// 4. Delete Long Click (BottomSheetDialog)
				v.setOnLongClickListener(view -> {
					int currentPos = holder.getAdapterPosition();
					if (currentPos == androidx.recyclerview.widget.RecyclerView.NO_POSITION) return true;
					
					com.google.android.material.bottomsheet.BottomSheetDialog bottomDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
					
					// Programmatic Layout for Bottom Sheet
					android.widget.LinearLayout sheetLayout = new android.widget.LinearLayout(getContext());
					sheetLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
					int pad = (int)(24 * getResources().getDisplayMetrics().density);
					sheetLayout.setPadding(pad, pad, pad, pad);
					sheetLayout.setBackgroundColor(android.graphics.Color.parseColor("#131B2D")); // Match App Theme
					
					// Title
					android.widget.TextView titleTv = new android.widget.TextView(getContext());
					titleTv.setText("Delete Recording?");
					titleTv.setTextSize(20f);
					titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
					titleTv.setTextColor(android.graphics.Color.WHITE);
					sheetLayout.addView(titleTv);
					
					// Message
					android.widget.TextView msgTv = new android.widget.TextView(getContext());
					msgTv.setText("Are you sure you want to delete this audio file forever? This cannot be undone.");
					msgTv.setTextSize(14f);
					msgTv.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
					android.widget.LinearLayout.LayoutParams msgParams = new android.widget.LinearLayout.LayoutParams(
					android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
					msgParams.setMargins(0, (int)(8 * getResources().getDisplayMetrics().density), 0, (int)(24 * getResources().getDisplayMetrics().density));
					msgTv.setLayoutParams(msgParams);
					sheetLayout.addView(msgTv);
					
					// Delete Button
					android.widget.Button deleteBtn = new android.widget.Button(getContext());
					deleteBtn.setText("Delete Forever");
					deleteBtn.setTextColor(android.graphics.Color.WHITE);
					deleteBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF4B4B"))); // Red color
					deleteBtn.setAllCaps(false);
					sheetLayout.addView(deleteBtn);
					
					deleteBtn.setOnClickListener(delView -> {
						java.util.HashMap<String, Object> deleteItem = displayList.get(currentPos);
						String path = deleteItem.containsKey("path") ? deleteItem.get("path").toString() : "";
						
						if(!path.isEmpty()){
							new java.io.File(path).delete(); 
						}
						
						if(currentlyPlayingPos == currentPos && mediaPlayer != null){
							mediaPlayer.stop();
							currentlyPlayingPos = -1;
						}
						
						String ts = deleteItem.get("timestamp").toString();
						displayList.remove(currentPos);
						
						for(int i=0; i<allList.size(); i++){
							if(allList.get(i).get("timestamp").equals(ts)){
								allList.remove(i);
								break;
							}
						}
						
						sp2.edit().putString("library_list", new com.google.gson.Gson().toJson(allList)).apply();
						
						notifyItemRemoved(currentPos);
						
						if(displayList.isEmpty()){
							binding.recyclerviewHistory.setVisibility(android.view.View.GONE);
							binding.linear4.setVisibility(android.view.View.VISIBLE);
							binding.historyStatusTv.setText(isFavTab ? "No favorites yet" : "No history found");
						}
						bottomDialog.dismiss();
					});
					
					bottomDialog.setContentView(sheetLayout);
					
					// Fix bottom sheet background corners
					android.view.View parentView = (android.view.View) sheetLayout.getParent();
					if (parentView != null) {
						parentView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
					}
					
					bottomDialog.show();
					return true; 
				});
			}
			
			@Override
			public int getItemCount() { return displayList.size(); }
		};
		
		binding.recyclerviewHistory.setAdapter(adapter);
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// SharedPreferences se fresh data uthao
		String libraryData = sp2.getString("library_list", "[]");
		
		allList = new com.google.gson.Gson().fromJson(libraryData, new com.google.gson.reflect.TypeToken<java.util.ArrayList<java.util.HashMap<String, Object>>>(){}.getType());
		if (allList == null) allList = new java.util.ArrayList<>();
		
		// List ko clear karke naye data se bharo
		displayList.clear();
		
		if (isFavTab) {
			for (java.util.HashMap<String, Object> item : allList) {
				boolean favCheck = item.containsKey("is_favorite") && item.get("is_favorite") != null && String.valueOf(item.get("is_favorite")).equals("true");
				if (favCheck) {
					displayList.add(item);
				}
			}
		} else {
			displayList.addAll(allList);
		}
		
		// UI Empty State Handle karo
		if (displayList.isEmpty()) {
			binding.recyclerviewHistory.setVisibility(android.view.View.GONE);
			binding.linear4.setVisibility(android.view.View.VISIBLE);
			binding.historyStatusTv.setText(isFavTab ? "No favorites yet" : "No history found");
		} else {
			binding.recyclerviewHistory.setVisibility(android.view.View.VISIBLE);
			binding.linear4.setVisibility(android.view.View.GONE);
		}
		
		// Adapter ko batao ki data change ho gaya hai
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
}