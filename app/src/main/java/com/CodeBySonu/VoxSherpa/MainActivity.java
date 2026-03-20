package com.CodeBySonu.VoxSherpa;

import android.animation.*;
import android.app.*;
import android.content.*;
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
import com.CodeBySonu.VoxSherpa.databinding.*;
import com.k2fsa.sherpa.onnx.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig;
import com.k2fsa.sherpa.onnx.GeneratedAudio;

public class MainActivity extends AppCompatActivity {
	
	private MainBinding binding;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
	}
	
	private void initializeLogic() {
		getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		// 1. SETUP VIEWPAGER2 ADAPTER (Inline)
		binding.viewpager.setAdapter(new androidx.viewpager2.adapter.FragmentStateAdapter(this) {
			@androidx.annotation.NonNull
			@Override
			public androidx.fragment.app.Fragment createFragment(int position) {
				switch (position) {
					case 0: return new GenerateFragmentActivity();
					case 1: return new ModelsFragmentActivity();
					case 2: return new LibraryFragmentActivity();
					case 3: return new SettingFragmentActivity();
					default: return new GenerateFragmentActivity();
				}
			}
			
			@Override
			public int getItemCount() {
				return 4; // Total 4 Tabs
			}
		});
		
		// 2. STATE PRESERVATION (Instagram Trick: Fragments destroy nahi honge)
		binding.viewpager.setOffscreenPageLimit(3); 
		
		// 3. SYNC UI WITH SWIPES & CLICKS
		// Jab bhi page change hoga (chahe swipe se ya click se), ye UI update kar dega
		binding.viewpager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				
				int inactiveColor = android.graphics.Color.parseColor("#8E99AF");
				int activeColor = android.graphics.Color.parseColor("#1D61FF");
				
				// Step A: Sabhi Tabs ko Pehle Inactive (Reset) kar do
				binding.bgGenerate.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
				binding.bgModels.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
				binding.bgLibrary.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
				binding.bgSettings.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
				
				binding.iconGenerate.setColorFilter(inactiveColor);
				binding.iconModels.setColorFilter(inactiveColor);
				binding.iconLibrary.setColorFilter(inactiveColor);
				binding.iconSettings.setColorFilter(inactiveColor);
				
				binding.txtGenerate.setTextColor(inactiveColor);
				binding.txtModels.setTextColor(inactiveColor);
				binding.txtLibrary.setTextColor(inactiveColor);
				binding.txtSettings.setTextColor(inactiveColor);
				
				// Step B: Jo Page khula hai, Sirf usko Active (Highlight) karo
				if (position == 0) {
					binding.bgGenerate.setCardBackgroundColor(activeColor);
					binding.iconGenerate.setColorFilter(android.graphics.Color.WHITE);
					binding.txtGenerate.setTextColor(android.graphics.Color.WHITE);
				} else if (position == 1) {
					binding.bgModels.setCardBackgroundColor(activeColor);
					binding.iconModels.setColorFilter(android.graphics.Color.WHITE);
					binding.txtModels.setTextColor(android.graphics.Color.WHITE);
				} else if (position == 2) {
					binding.bgLibrary.setCardBackgroundColor(activeColor);
					binding.iconLibrary.setColorFilter(android.graphics.Color.WHITE);
					binding.txtLibrary.setTextColor(android.graphics.Color.WHITE);
				} else if (position == 3) {
					binding.bgSettings.setCardBackgroundColor(activeColor);
					binding.iconSettings.setColorFilter(android.graphics.Color.WHITE);
					binding.txtSettings.setTextColor(android.graphics.Color.WHITE);
				}
			}
		});
		
		// 4. BOTTOM NAV CLICK LISTENERS (Instant Switch Trick)
		// setCurrentItem(index, false) -> 'false' se sliding animation off ho jayega
		binding.navGenerate.setOnClickListener(v -> {
			binding.viewpager.setCurrentItem(0, false); 
		});
		
		binding.navModels.setOnClickListener(v -> {
			binding.viewpager.setCurrentItem(1, false);
		});
		
		binding.navLibrary.setOnClickListener(v -> {
			binding.viewpager.setCurrentItem(2, false);
		});
		
		binding.navSettings.setOnClickListener(v -> {
			binding.viewpager.setCurrentItem(3, false);
		});
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		
		// Jab poori app band ho rahi ho, tabhi C++ (JNI) Session ko free karna hai
		com.CodeBySonu.VoxSherpa.VoiceEngine.getInstance().destroy();
	}
	
}