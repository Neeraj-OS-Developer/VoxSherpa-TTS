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
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.snackbar.Snackbar;
import android.os.Process;

public class GenerateFragmentActivity extends Fragment {
	
	private GenerateFragmentBinding binding;
	private boolean isAudioGeneratedForCurrentText = false;
	private String lastGeneratedText = "";
	private byte[] lastGeneratedPcmData = null;
	private android.media.AudioTrack audioTrack;
	private android.animation.ValueAnimator playheadAnimator;
	androidx.appcompat.widget.ListPopupWindow listPopupWindow;
	android.widget.ArrayAdapter<String> voiceAdapter;
	private int lastGeneratedSampleRate = 22050;
	private volatile boolean isCancelled = false;
	private volatile boolean isGenerating = false;
	
	private SharedPreferences sp1;
	private SharedPreferences sp2;
	private SharedPreferences sp3;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		binding = GenerateFragmentBinding.inflate(_inflater, _container, false);
		initialize(_savedInstanceState, binding.getRoot());
		initializeLogic();
		return binding.getRoot();
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		sp1 = getContext().getSharedPreferences("sp1", Activity.MODE_PRIVATE);
		sp2 = getContext().getSharedPreferences("sp2", Activity.MODE_PRIVATE);
		sp3 = getContext().getSharedPreferences("sp3", Activity.MODE_PRIVATE);
		
		binding.btnGenerate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				// Fragment Code (Start)
				String inputText = binding.etInput.getText().toString().trim();
				
				if (inputText.isEmpty()) {
					if (getView() != null) {
						com.google.android.material.snackbar.Snackbar.make(getView(), "Please enter some text first.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
						.setBackgroundTint(Color.parseColor("#FF4B4B")).setTextColor(Color.WHITE).show();
					}
					return;
				}
				
				String currentOnnxModel = sp1.getString("active_model", "");
				String currentTokens    = sp1.getString("active_tokens", "");
				String currentModelType = sp1.getString("active_model_type", "vits");
				String currentVoicesBin = sp1.getString("active_voices_bin", "");
				
				boolean isPunctOn   = sp3.getBoolean("smart_punct", false);
				boolean isEmotionOn = sp3.getBoolean("emotion_tags", false);
				float currentSpeed  = sp3.getFloat("voice_speed", 1.0f);
				float currentPitch  = sp3.getFloat("voice_pitch", 1.0f);
				
				if (currentOnnxModel.isEmpty() || currentTokens.isEmpty()) {
					if (getView() != null) {
						com.google.android.material.snackbar.Snackbar.make(getView(), "Please select a Voice Model from Models tab.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
						.setBackgroundTint(Color.parseColor("#FF4B4B")).setTextColor(Color.WHITE).show();
					}
					return;
				}
				
				// --- ACTION: CANCEL ---
				if (isGenerating) {
					if (!isCancelled) { 
						new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
						.setTitle("Cancel Generation")
						.setMessage("Are you sure you want to cancel the voice synthesis?")
						.setPositiveButton("Yes, Cancel", (dialog, which) -> {
							isCancelled = true; 
							binding.textview92.setText("Canceling...");
							binding.btnGenerate.setAlpha(0.5f);
							binding.btnGenerate.setEnabled(false); 
							dialog.dismiss();
						})
						.setNegativeButton("No", (dialog, which) -> dialog.dismiss())
						.show();
					}
					return;
				}
				
				if (isAudioGeneratedForCurrentText) {
					// --- ACTION: PLAY / PAUSE ---
					if (lastGeneratedPcmData != null && audioTrack != null) {
						int playState = audioTrack.getPlayState();
						int currentHead = audioTrack.getPlaybackHeadPosition();
						int totalFrames = lastGeneratedPcmData.length / 2;
						
						if (playState == AudioTrack.PLAYSTATE_PLAYING) {
							// PAUSE LOGIC
							audioTrack.pause();
							binding.imageview52.setImageResource(R.drawable.icon_play_circle);
							binding.textview92.setText("Play");
							if (playheadAnimator != null) playheadAnimator.pause();
						} else {
							// 🚀 FIX: BULLETPROOF RE-PLAY LOGIC (0% se start hoga aur properly bajega)
							if (currentHead >= totalFrames - 200 || playState == AudioTrack.PLAYSTATE_STOPPED) {
								try {
									audioTrack.stop();
									audioTrack.reloadStaticData();
								} catch (Exception ignored) {}
								
								binding.playheadLine.setTranslationX(0f);
								int w = binding.imgWaveform.getWidth() > 0 ? binding.imgWaveform.getWidth() : 800;
								double totalSeconds = (double) totalFrames / lastGeneratedSampleRate;
								
								if (playheadAnimator != null) playheadAnimator.cancel();
								
								// Naya animator banega poore time ke liye taaki 30% wala bug na aaye
								playheadAnimator = ValueAnimator.ofFloat(0f, (float) w);
								playheadAnimator.setDuration((long) (totalSeconds * 1000));
								playheadAnimator.setInterpolator(new LinearInterpolator());
								playheadAnimator.addUpdateListener(anim ->
								binding.playheadLine.setTranslationX((float) anim.getAnimatedValue())
								);
								
								// 🚀 FIX: End Event Listener wapas joda
								playheadAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
									@Override
									public void onAnimationEnd(android.animation.Animator animation) {
										if (binding.playheadLine.getTranslationX() >= w - 10) {
											binding.imageview52.setImageResource(R.drawable.icon_play_circle);
											binding.textview92.setText("Play");
											binding.playheadLine.setTranslationX(0f);
											try {
												if (audioTrack != null) {
													audioTrack.stop();
													audioTrack.reloadStaticData();
												}
											} catch (Exception ignored) {}
										}
									}
								});
							}
							
							audioTrack.play();
							binding.imageview52.setImageResource(R.drawable.icon_pause_circle);
							binding.textview92.setText("Pause");
							
							if (playheadAnimator != null) {
								if (playheadAnimator.isPaused()) {
									playheadAnimator.resume();
								} else {
									playheadAnimator.start();
								}
							}
						}
					}
				} else {
					// --- ACTION: GENERATE ---
					
					isGenerating = true;
					isCancelled = false;
					binding.btnGenerate.setEnabled(true); 
					binding.btnGenerate.setAlpha(1.0f);
					
					binding.imageview65.setVisibility(View.GONE);
					binding.progressGenerating.setVisibility(View.VISIBLE);
					binding.textview69.setTextColor(Color.parseColor("#1D61FF"));
					
					binding.textview92.setText("Cancel");
					binding.imageview52.setImageResource(R.drawable.ic_close); 
					
					final boolean isKokoro = currentModelType.equals("kokoro");
					
					List<String> sentences = new ArrayList<>();
					String[] parts = inputText.split("(?<=[.!?\\n|।])\\s+");
					for (String part : parts) {
						if (!part.trim().isEmpty()) {
							sentences.add(part.trim());
						}
					}
					final int totalSentences = sentences.size();
					if (totalSentences == 0) sentences.add(inputText);
					
					binding.textview69.setText("GENERATING VOICE... 0/" + totalSentences);
					
					new Thread(() -> {
						Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
						
						String loadResult = "";
						byte[] finalGeneratedPcm = null;
						int generatedSampleRate = 24000; 
						
						ByteArrayOutputStream pcmStream = new ByteArrayOutputStream();
						
						if (isKokoro) {
							loadResult = com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().loadModel(
							getContext(), currentOnnxModel, currentTokens, currentVoicesBin
							);
							if ("Success".equals(loadResult)) generatedSampleRate = com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().getSampleRate();
						} else {
							loadResult = com.CodeBySonu.VoxSherpa.VoiceEngine.getInstance().loadModel(
							getContext(), currentOnnxModel, currentTokens
							);
							if ("Success".equals(loadResult)) generatedSampleRate = com.CodeBySonu.VoxSherpa.VoiceEngine.getInstance().getSampleRate();
						}
						
						if (generatedSampleRate <= 0) generatedSampleRate = isKokoro ? 24000 : 22050;
						
						final int[] playedFramesRef = {0};
						
						if ("Success".equals(loadResult)) {
							int minBufferSize = AudioTrack.getMinBufferSize(
							generatedSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
							);
							final AudioTrack liveStreamTrack = new AudioTrack(
							AudioManager.STREAM_MUSIC, generatedSampleRate,
							AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
							minBufferSize, AudioTrack.MODE_STREAM
							);
							
							java.util.concurrent.LinkedBlockingQueue<byte[]> audioQueue = new java.util.concurrent.LinkedBlockingQueue<>();
							
							Thread playerThread = new Thread(() -> {
								Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
								try {
									liveStreamTrack.play();
									while (true) {
										if (isCancelled) break;
										byte[] chunk = audioQueue.take();
										if (chunk.length == 0) break; 
										
										int written = liveStreamTrack.write(chunk, 0, chunk.length);
										if (written < 0) break; 
									}
								} catch (Exception e) {}
							});
							playerThread.start();
							
							int doneCount = 0;
							
							for (String sentence : sentences) {
								if (isCancelled) break;
								
								byte[] chunkData = null;
								
								if (isKokoro) {
									chunkData = com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().generateAudioPCM(sentence, currentSpeed, currentPitch);
								} else {
									if (isPunctOn || isEmotionOn) {
										chunkData = com.CodeBySonu.VoxSherpa.AudioEmotionHelper.processAndGenerate(
										sentence, isPunctOn, isEmotionOn, currentSpeed, currentPitch, 1.0f
										);
									} else {
										chunkData = com.CodeBySonu.VoxSherpa.VoiceEngine.getInstance().generateAudioPCM(sentence, currentSpeed, currentPitch);
									}
								}
								
								if (isCancelled) break;
								
								if (chunkData != null && chunkData.length > 0) {
									try {
										pcmStream.write(chunkData);
										audioQueue.put(chunkData); 
									} catch (Exception ignored) {}
								}
								
								doneCount++;
								final int current = doneCount;
								if (getActivity() != null) {
									getActivity().runOnUiThread(() -> {
										if (!isCancelled) {
											binding.textview69.setText("GENERATING VOICE... " + current + "/" + totalSentences);
										}
									});
								}
							} 
							
							finalGeneratedPcm = pcmStream.toByteArray();
							
							if (!isCancelled) {
								try {
									playedFramesRef[0] = liveStreamTrack.getPlaybackHeadPosition();
									audioQueue.clear();
									audioQueue.put(new byte[0]);
									
									liveStreamTrack.pause();
									liveStreamTrack.flush();
									liveStreamTrack.release();
								} catch (Exception e) {}
							}
						}
						
						final byte[] finalPcm = finalGeneratedPcm;
						final String finalLoadResult = loadResult;
						final int finalSampleRate = generatedSampleRate;
						
						if (getActivity() != null && isAdded()) {
							getActivity().runOnUiThread(() -> {
								isGenerating = false; 
								binding.btnGenerate.setEnabled(true);
								binding.btnGenerate.setAlpha(1.0f);
								
								if (isCancelled) {
									binding.progressGenerating.setVisibility(View.GONE);
									binding.imageview65.setVisibility(View.VISIBLE);
									binding.textview69.setText("CANCELED BY USER");
									binding.textview69.setTextColor(Color.parseColor("#718096")); 
									
									binding.textview92.setText("Generate");
									binding.imageview52.setImageResource(R.drawable.icon_play_circle); 
									return; 
								}
								
								if ("Success".equals(finalLoadResult) && finalPcm != null && finalPcm.length > 0) {
									lastGeneratedText = inputText;
									lastGeneratedPcmData = finalPcm;
									lastGeneratedSampleRate = finalSampleRate;
									isAudioGeneratedForCurrentText = true;
									
									binding.layoutIdleState.setVisibility(View.GONE);
									binding.layoutGeneratedState.setVisibility(View.VISIBLE);
									
									double seconds = (finalPcm.length / 2.0) / finalSampleRate;
									int min = (int) (seconds / 60);
									int sec = (int) (seconds % 60);
									binding.tvDuration.setText(String.format(Locale.US, "%d:%02d seconds", min, sec));
									
									int w = binding.imgWaveform.getWidth() > 0 ? binding.imgWaveform.getWidth() : 800;
									Bitmap waveBmp = com.CodeBySonu.VoxSherpa.WaveformHelper.createWaveformBitmap(finalPcm, w, 150);
									if (waveBmp != null) binding.imgWaveform.setImageBitmap(waveBmp);
									
									try {
										if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
											try { audioTrack.stop(); } catch (Exception ignored) {}
											audioTrack.release();
										}
										
										int totalFrames = finalPcm.length / 2;
										audioTrack = new AudioTrack(
										AudioManager.STREAM_MUSIC, finalSampleRate,
										AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
										finalPcm.length, AudioTrack.MODE_STATIC
										);
										audioTrack.write(finalPcm, 0, finalPcm.length);
										
										int playedFrames = playedFramesRef[0];
										if (playedFrames < 0) playedFrames = 0;
										if (playedFrames > totalFrames) playedFrames = totalFrames;
										
										audioTrack.setPlaybackHeadPosition(playedFrames);
										audioTrack.play(); 
										
										float startX = ((float) playedFrames / totalFrames) * w;
										
										binding.playheadLine.setVisibility(View.VISIBLE);
										binding.playheadLine.setTranslationX(startX);
										
										if (playheadAnimator != null) playheadAnimator.cancel();
										
										float remainingSeconds = (float) (totalFrames - playedFrames) / finalSampleRate;
										if (remainingSeconds < 0) remainingSeconds = 0f;
										
										playheadAnimator = ValueAnimator.ofFloat(startX, (float) w);
										playheadAnimator.setDuration((long) (remainingSeconds * 1000));
										playheadAnimator.setInterpolator(new LinearInterpolator());
										playheadAnimator.addUpdateListener(anim ->
										binding.playheadLine.setTranslationX((float) anim.getAnimatedValue())
										);
										
										// 🚀 FIX: First Time End Animator Listener joda taaki naturally finish ho
										playheadAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
											@Override
											public void onAnimationEnd(android.animation.Animator animation) {
												if (binding.playheadLine.getTranslationX() >= w - 10) {
													binding.imageview52.setImageResource(R.drawable.icon_play_circle);
													binding.textview92.setText("Play");
													binding.playheadLine.setTranslationX(0f);
													try {
														if (audioTrack != null) {
															audioTrack.stop();
															audioTrack.reloadStaticData();
														}
													} catch (Exception ignored) {}
												}
											}
										});
										
										playheadAnimator.start();
										
										// Fallback marker update listener
										audioTrack.setNotificationMarkerPosition(totalFrames);
										audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
											@Override
											public void onMarkerReached(AudioTrack track) {
												if (getActivity() != null) {
													getActivity().runOnUiThread(() -> {
														binding.imageview52.setImageResource(R.drawable.icon_play_circle);
														binding.textview92.setText("Play");
														binding.playheadLine.setTranslationX(0f);
														if (playheadAnimator != null) playheadAnimator.cancel();
														try {
															track.stop();
															track.reloadStaticData();
														} catch (Exception ignored) {}
													});
												}
											}
											@Override
											public void onPeriodicNotification(AudioTrack track) {}
										});
										
										binding.imageview52.setImageResource(R.drawable.icon_pause_circle);
										binding.textview92.setText("Pause");
										
									} catch (Throwable t) {
									}
									
								} else {
									binding.progressGenerating.setVisibility(View.GONE);
									binding.imageview65.setVisibility(View.VISIBLE);
									binding.textview69.setText("SYNTHESIS FAILED");
									binding.textview69.setTextColor(Color.parseColor("#FF4B4B"));
									
									binding.textview92.setText("Generate");
									binding.imageview52.setImageResource(R.drawable.icon_play_circle); 
									
									if (getView() != null) {
										String userFriendlyMessage;
										if (finalLoadResult != null && (finalLoadResult.contains("missing") || finalLoadResult.contains("empty"))) {
											userFriendlyMessage = "Please select a valid voice model to continue.";
										} else {
											userFriendlyMessage = "Unable to generate voice at the moment. Please try again.";
										}
										Snackbar.make(getView(), userFriendlyMessage, Snackbar.LENGTH_LONG)
										.setBackgroundTint(Color.parseColor("#FF4B4B"))
										.setTextColor(Color.WHITE)
										.show();
									}
								}
							});
						}
					}).start();
				}
				
			}
		});
		
		binding.save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_saveAudioAction();
			}
		});
		
		binding.etInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				// Yahan humne s ki jagah Sketchware ka _charSeq use kiya hai
				int length = _charSeq.length();
				
				if (length > 1000) {
					binding.etInput.setText(_charSeq.substring(0, 1000));
					binding.etInput.setSelection(1000); 
					length = 1000;
				}
				
				binding.inputCountTv.setText(length + "/1000");
				
				if (length >= 1000) {
					binding.inputCountTv.setTextColor(android.graphics.Color.parseColor("#FF4B4B"));
				} else {
					binding.inputCountTv.setTextColor(android.graphics.Color.parseColor("#414D63"));
				}
				
				// Reset state on text change
				String currentText = binding.etInput.getText().toString().trim();
				
				// 🚀 FIX: UI state ko tabhi reset karo jab generation process idle ho (!isGenerating)
				if (!currentText.equals(lastGeneratedText) && !isGenerating) {
					isAudioGeneratedForCurrentText = false;
					
					// UI STATE RESET
					binding.layoutIdleState.setVisibility(android.view.View.VISIBLE);
					binding.layoutGeneratedState.setVisibility(android.view.View.GONE);
					binding.progressGenerating.setVisibility(android.view.View.GONE);
					binding.imageview65.setVisibility(android.view.View.VISIBLE);
					
					binding.textview92.setText("Generate & Play");
					binding.imageview52.setImageResource(R.drawable.icon_play_circle); 
					
					binding.textview69.setText("READY TO SYNTHESIZE");
					binding.textview69.setTextColor(android.graphics.Color.parseColor("#3F4B61"));
					
					if (audioTrack != null && audioTrack.getState() != android.media.AudioTrack.STATE_UNINITIALIZED) {
						try { audioTrack.stop(); } catch (Exception e) {}
					}
					if (playheadAnimator != null) playheadAnimator.cancel();
				}
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		binding.cardPaste.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
				if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
					CharSequence pasteData = clipboard.getPrimaryClip().getItemAt(0).getText();
					if (pasteData != null) {
						binding.etInput.setText(pasteData.toString());
					}
				} else {
					if (getView() != null) {
						com.google.android.material.snackbar.Snackbar.make(getView(), "Clipboard is empty", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
						.setBackgroundTint(android.graphics.Color.parseColor("#3F4B61"))
						.setTextColor(android.graphics.Color.WHITE)
						.show();
					}
				}
				
			}
		});
		
		binding.opneDropdown.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
				
				String activeName = sp1.getString("active_model_name", "");
				boolean kokoroActive = activeName.toLowerCase().contains("kokoro");
				String activePath = sp1.getString("active_model", "");
				
				if (activePath.isEmpty()) return;
				
				if (!kokoroActive) {
					com.google.android.material.snackbar.Snackbar
					.make(_view, "Piper model has only one voice.",
					com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
					.setBackgroundTint(android.graphics.Color.parseColor("#3F4B61"))
					.setTextColor(android.graphics.Color.WHITE)
					.show();
					return;
				}
				
				if (listPopupWindow != null) {
					if (listPopupWindow.isShowing()) {
						listPopupWindow.dismiss();
					} else {
						int savedSpeakerId = sp1.getInt("active_kokoro_speaker", 31);
						java.util.List<com.CodeBySonu.VoxSherpa.KokoroVoiceHelper.VoiceItem> voices =
						com.CodeBySonu.VoxSherpa.KokoroVoiceHelper.getAllVoices();
						
						for (int i = 0; i < voices.size(); i++) {
							if (voices.get(i).speakerId == savedSpeakerId) {
								listPopupWindow.setSelection(i);
								break;
							}
						}
						listPopupWindow.show();
					}
				}
			}
		});
		
		binding.btnExport.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_saveAudioAction();
			}
		});
	}
	
	private void initializeLogic() {
		String activeModel = sp1.getString("active_model", "");
		String activeModelName = sp1.getString("active_model_name", "");
		String activeModelType = sp1.getString("active_model_type", "");
		
		// Safety check for Kokoro
		boolean isKokoro = activeModelType.equals("kokoro") || activeModelName.toLowerCase().contains("kokoro");
		
		if (activeModel.isEmpty()) {
			binding.voiceNameTv.setText("No Model Selected");
			binding.voiceNameTv.setTextColor(android.graphics.Color.parseColor("#FF4B4B"));
		} else {
			if (isKokoro) {
				// Kokoro logic (Bilkul untouched, jaisa tha waisa hi perfect hai)
				try {
					int savedSpeakerId = sp1.getInt("active_kokoro_speaker", 31);
					com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().setActiveSpeakerId(savedSpeakerId);
					String voiceName = com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().getActiveVoiceName();
					binding.voiceNameTv.setText((voiceName != null && !voiceName.isEmpty()) ? voiceName : "Kokoro Voice");
				} catch (Throwable t) {
					binding.voiceNameTv.setText("Kokoro Voice");
				}
			} else {
				// 🚀 PIPER FIX: File name ya purane string replace logic ki jagah sidha JSON database se real info nikalna
				String piperLang = "Unknown";
				String piperGender = "Unknown";
				
				try {
					String modelsDataRaw = sp1.getString("models_data", "[]");
					if (!modelsDataRaw.equals("[]")) {
						java.util.ArrayList<java.util.HashMap<String, Object>> mList = 
						new com.google.gson.Gson().fromJson(modelsDataRaw, 
						new com.google.gson.reflect.TypeToken<java.util.ArrayList<java.util.HashMap<String, Object>>>(){}.getType());
						
						if (mList != null) {
							for (java.util.HashMap<String, Object> model : mList) {
								// Check exact ONNX path match
								String onnxPath = model.containsKey("onnx_path") && model.get("onnx_path") != null ? model.get("onnx_path").toString() : "";
								
								if (activeModel.equals(onnxPath)) {
									// Extract exact Language and Gender
									if (model.containsKey("language") && model.get("language") != null) {
										piperLang = model.get("language").toString().trim();
									}
									if (model.containsKey("gender") && model.get("gender") != null) {
										piperGender = model.get("gender").toString().trim();
									}
									break; 
								}
							}
						}
					}
				} catch (Exception e) {} 
				
				// First letter ko capital karna (Formatting)
				if (!piperLang.equals("Unknown") && piperLang.length() > 0) {
					piperLang = piperLang.substring(0, 1).toUpperCase() + piperLang.substring(1).toLowerCase();
				}
				if (!piperGender.equals("Unknown") && piperGender.length() > 0) {
					piperGender = piperGender.substring(0, 1).toUpperCase() + piperGender.substring(1).toLowerCase();
				}
				
				// Perfect format set karna
				binding.voiceNameTv.setText("Piper • " + piperGender + " • " + piperLang);
			}
			binding.voiceNameTv.setTextColor(android.graphics.Color.WHITE);
		}
		
		// Dropdown Logic (Untouched & Safe)
		listPopupWindow = new androidx.appcompat.widget.ListPopupWindow(getContext());
		listPopupWindow.setAnchorView(binding.voiceNameTv);
		listPopupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#131B2D")));
		
		java.util.List<com.CodeBySonu.VoxSherpa.KokoroVoiceHelper.VoiceItem> allVoices = com.CodeBySonu.VoxSherpa.KokoroVoiceHelper.getAllVoices();
		java.util.List<String> voiceNames = new java.util.ArrayList<>();
		for (com.CodeBySonu.VoxSherpa.KokoroVoiceHelper.VoiceItem vItem : allVoices) {
			try {
				String label = vItem.getFullLabel();
				voiceNames.add((label != null && !label.isEmpty()) ? label : "Unknown Voice");
			} catch (Throwable t) {
				voiceNames.add("Unknown Voice");
			}
		}
		
		voiceAdapter = new android.widget.ArrayAdapter<>(getContext(), R.layout.dropdown_item, R.id.text1, voiceNames);
		listPopupWindow.setAdapter(voiceAdapter);
		
		listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
			try {
				String selectedLabel = voiceAdapter.getItem(position);
				if (selectedLabel == null) return;
				binding.voiceNameTv.setText(selectedLabel);
				for (com.CodeBySonu.VoxSherpa.KokoroVoiceHelper.VoiceItem vItem : allVoices) {
					try {
						if (selectedLabel.equals(vItem.getFullLabel())) {
							com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().setActiveSpeakerId(vItem.speakerId);
							sp1.edit().putInt("active_kokoro_speaker", vItem.speakerId).apply();
							break;
						}
					} catch (Throwable t) {}
				}
			} catch (Throwable t) {}
			listPopupWindow.dismiss();
		});
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		// Fragment me sirf AudioTrack aur UI Animator ko release karna hai
		if (audioTrack != null) {
			try {
				if (audioTrack.getState() != android.media.AudioTrack.STATE_UNINITIALIZED) {
					audioTrack.stop();
				}
				audioTrack.release();
				audioTrack = null;
			} catch (Exception ignored) {}
		}
		
		if (playheadAnimator != null) {
			playheadAnimator.cancel();
			playheadAnimator = null;
		}
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		String activeModel = sp1.getString("active_model", "");
		String activeModelType = sp1.getString("active_model_type", "");
		String activeModelName = sp1.getString("active_model_name", "");
		
		boolean isKokoro = activeModelType.equals("kokoro") || activeModelName.toLowerCase().contains("kokoro");
		
		if (activeModel.isEmpty()) {
			binding.voiceNameTv.setText("No Model Selected");
			binding.voiceNameTv.setTextColor(android.graphics.Color.parseColor("#FF4B4B"));
		} else {
			if (isKokoro) {
				// Kokoro Logic (Untouched & Perfect)
				try {
					int savedSpeakerId = sp1.getInt("active_kokoro_speaker", 31);
					com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().setActiveSpeakerId(savedSpeakerId);
					String voiceName = com.CodeBySonu.VoxSherpa.KokoroEngine.getInstance().getActiveVoiceName();
					binding.voiceNameTv.setText((voiceName != null && !voiceName.isEmpty()) ? voiceName : "Kokoro Voice");
				} catch (Throwable t) {
					binding.voiceNameTv.setText("Kokoro Voice");
				}
			} else {
				// 🚀 PIPER FIX: Parse JSON from SharedPreferences to extract Exact Gender and Language
				String piperLang = "Unknown";
				String piperGender = "Unknown";
				
				try {
					String modelsDataRaw = sp1.getString("models_data", "[]");
					if (!modelsDataRaw.equals("[]")) {
						java.util.ArrayList<java.util.HashMap<String, Object>> mList = 
						new com.google.gson.Gson().fromJson(modelsDataRaw, 
						new com.google.gson.reflect.TypeToken<java.util.ArrayList<java.util.HashMap<String, Object>>>(){}.getType());
						
						if (mList != null) {
							for (java.util.HashMap<String, Object> model : mList) {
								// Check exact ONNX path match
								String onnxPath = model.containsKey("onnx_path") && model.get("onnx_path") != null ? model.get("onnx_path").toString() : "";
								
								if (activeModel.equals(onnxPath)) {
									// Extract Language and Gender
									if (model.containsKey("language") && model.get("language") != null) {
										piperLang = model.get("language").toString().trim();
									}
									if (model.containsKey("gender") && model.get("gender") != null) {
										piperGender = model.get("gender").toString().trim();
									}
									break; // Match found, break loop
								}
							}
						}
					}
				} catch (Exception e) {} // Ignore parse errors safely
				
				// Format to strict "Piper • Gender • Language" syntax
				
				// Safety capitalization
				if (!piperLang.equals("Unknown") && piperLang.length() > 0) {
					piperLang = piperLang.substring(0, 1).toUpperCase() + piperLang.substring(1).toLowerCase();
				}
				if (!piperGender.equals("Unknown") && piperGender.length() > 0) {
					piperGender = piperGender.substring(0, 1).toUpperCase() + piperGender.substring(1).toLowerCase();
				}
				
				binding.voiceNameTv.setText("Piper • " + piperGender + " • " + piperLang);
			}
			binding.voiceNameTv.setTextColor(android.graphics.Color.WHITE);
		}
		
	}
	
	public void _saveAudioAction() {
		// ─── SAVE BUTTON LOGIC ──────────────────────────────────────────────────────
		
		if (!isAudioGeneratedForCurrentText || lastGeneratedPcmData == null) {
			if (getView() != null) {
				Snackbar.make(getView(), "Please generate audio first!", Snackbar.LENGTH_SHORT)
				.setBackgroundTint(Color.parseColor("#FF4B4B"))
				.setTextColor(Color.WHITE)
				.show();
			}
			return;
		}
		
		// ✅ Safety check — agar kisi wajah se 0 reh gaya to sane fallback
		int sampleRateToSave = lastGeneratedSampleRate > 0 ? lastGeneratedSampleRate : 22050;
		
		String cleanFileName = "Vox_" + System.currentTimeMillis() + ".wav";
		String savedPath = AudioHelper.saveWavFile(lastGeneratedPcmData, cleanFileName, sampleRateToSave, getContext());
		
		if (!savedPath.isEmpty()) {
			try {
				String libraryData = sp2.getString("library_list", "[]");
				ArrayList<HashMap<String, Object>> libList = new Gson().fromJson(
				libraryData, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType()
				);
				
				HashMap<String, Object> newItem = new HashMap<>();
				newItem.put("title", cleanFileName);
				newItem.put("text", lastGeneratedText);
				newItem.put("path", savedPath);
				
				String cleanDuration = binding.tvDuration.getText().toString().replace(" seconds", "");
				newItem.put("duration", cleanDuration);
				newItem.put("timestamp", String.valueOf(System.currentTimeMillis()));
				newItem.put("voice_name", binding.voiceNameTv.getText().toString());
				newItem.put("is_favorite", false);
				
				libList.add(0, newItem);
				sp2.edit().putString("library_list", new Gson().toJson(libList)).apply();
				
				if (getView() != null) {
					Snackbar.make(getView(), "Audio saved to Library!", Snackbar.LENGTH_SHORT)
					.setBackgroundTint(Color.parseColor("#1D61FF"))
					.setTextColor(Color.WHITE)
					.show();
				}
			} catch (Exception e) {
				if (getView() != null) {
					Snackbar.make(getView(), "Failed to update library data.", Snackbar.LENGTH_SHORT)
					.setBackgroundTint(Color.parseColor("#FF4B4B"))
					.setTextColor(Color.WHITE)
					.show();
				}
			}
		} else {
			if (getView() != null) {
				Snackbar.make(getView(), "Failed to save audio file.", Snackbar.LENGTH_SHORT)
				.setBackgroundTint(Color.parseColor("#FF4B4B"))
				.setTextColor(Color.WHITE)
				.show();
			}
		}
		
	}
	
}