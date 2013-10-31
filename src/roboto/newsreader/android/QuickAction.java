package roboto.newsreader.android;

import android.app.DownloadManager;
import android.content.Context;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.PopupWindow.OnDismissListener;

import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import java.util.List;
import java.util.ArrayList;

import com.roboto.database.EnglishDictionaryDatabaseManager;
import com.roboto.file.*;
import roboto.newsreader.*;

/**
 * QuickAction dialog, shows action list as icon and text like the one in Gallery3D app. Currently supports vertical 
 * and horizontal layout.
 * 
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 * 
 * Contributors:
 * - Kevin Peck <kevinwpeck@gmail.com>
 */
public class QuickAction extends PopupWindows implements OnDismissListener, FileStatusUpdateListener {
    private static final String TAG = QuickAction.class.getSimpleName();
    private static int ROOT_VIEW_WIDTH = 300;
    private static int ROOT_VIEW_HEIGHT = 300;
    private View mRootView;
	private ImageView mArrowUp;
	private ImageView mArrowDown;
	private LayoutInflater mInflater;
	private ViewGroup mTrack;
	private ViewGroup mScroller;
	private OnActionItemClickListener mItemClickListener;
	private OnDismissListener mDismissListener;
	
	private List<ActionItem> actionItems = new ArrayList<ActionItem>();
	
	private boolean mDidAction;
	
	private int mChildPos;
    private int mInsertPos;
    private int mAnimStyle;
    private int mOrientation;
    private int rootWidth=0;
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    
    public static final int ANIM_GROW_FROM_LEFT = 1;
	public static final int ANIM_GROW_FROM_RIGHT = 2;
	public static final int ANIM_GROW_FROM_CENTER = 3;
	public static final int ANIM_REFLECT = 4;
	public static final int ANIM_AUTO = 5;
    private ViewGroup downloadStatusView;
    private ViewGroup dictionaryMeaningView;
    private Button downloadButton;
    private String mSelectedText;

    /**
     * Constructor for default vertical layout
     * 
     * @param context  Context
     */
    public QuickAction(Context context) {
        this(context, VERTICAL);
    }

    /**
     * Constructor allowing orientation override
     * 
     * @param context    Context
     * @param orientation Layout orientation, can be vartical or horizontal
     */
    public QuickAction(Context context, int orientation) {
        super(context);
        
        mOrientation = orientation;
        
        mInflater 	 = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mOrientation == HORIZONTAL) {
            setRootViewId(R.layout.popup_horizontal);
        } else {
            setRootViewId(R.layout.popup_vertical_dictionary_meaning);
        }

        mAnimStyle 	= ANIM_AUTO;
        mChildPos 	= 0;

      //  ROOT_VIEW_WIDTH = (int)getDensityIndependentValue( 300f, context);
      //  ROOT_VIEW_HEIGHT = (int)getDensityIndependentValue( 300f, context);

    }

    /**
     * Get action item at an index
     * 
     * @param index  Index of item (position from callback)
     * 
     * @return  Action Item at the position
     */
    public ActionItem getActionItem(int index) {
        return actionItems.get(index);
    }
    
	/**
	 * Set root view.
	 * 
	 * @param id Layout resource id
	 */
	public void setRootViewId(int id) {
		mRootView	= (ViewGroup) mInflater.inflate(id, null);
		mTrack 		= (ViewGroup) mRootView.findViewById(R.id.tracks);

		mArrowDown 	= (ImageView) mRootView.findViewById(R.id.arrow_down);
		mArrowUp 	= (ImageView) mRootView.findViewById(R.id.arrow_up);

		mScroller	= (ViewGroup) mRootView.findViewById(R.id.scroller);

        downloadStatusView = (ViewGroup) mRootView.findViewById(R.id.popup_download_status_view);
        dictionaryMeaningView = (ViewGroup) mRootView.findViewById(R.id.popup_dictionary_meaning_view);

        downloadButton = (Button)downloadStatusView.findViewById(R.id.popup_download_status_layout_root)
                .findViewById(R.id.popup_download_status_download_button);
        downloadButton.setOnClickListener(downloadButtonListener);
		
		//This was previously defined on show() method, moved here to prevent force close that occured
		//when tapping fastly on a view to show quickaction dialog.
		//Thanx to zammbi (github.com/zammbi)
		mRootView.setLayoutParams(new LayoutParams(ROOT_VIEW_WIDTH, ROOT_VIEW_HEIGHT));
		
		setContentView(mRootView);
	}
	
	/**
	 * Set animation style
	 * 
	 * @param mAnimStyle animation style, default is set to ANIM_AUTO
	 */
	public void setAnimStyle(int mAnimStyle) {
		this.mAnimStyle = mAnimStyle;
	}
	
	/**
	 * Set listener for action item clicked.
	 * 
	 * @param listener Listener
	 */
	public void setOnActionItemClickListener(OnActionItemClickListener listener) {
		mItemClickListener = listener;
	}
	
	/**
	 * Add action item
	 * 
	 * @param action  {@link ActionItem}
	 */
	public void addActionItem(ActionItem action) {
		actionItems.add(action);
		
		String title 	= action.getTitle();
		Drawable icon 	= action.getIcon();
		
		View container;
		
		if (mOrientation == HORIZONTAL) {
            container = mInflater.inflate(R.layout.action_item_horizontal, null);
        } else {
            container = mInflater.inflate(R.layout.action_item_horizontal, null);
        }
		
		ImageView img 	= (ImageView) container.findViewById(R.id.iv_icon);
		TextView text 	= (TextView) container.findViewById(R.id.tv_title);
		
		if (icon != null) {
			img.setImageDrawable(icon);
		} else {
			img.setVisibility(View.GONE);
		}
		
		if (title != null) {
			text.setText(title);
		} else {
			text.setVisibility(View.GONE);
		}
		
		final int pos 		=  mChildPos;
		final int actionId 	= action.getActionId();
		
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(QuickAction.this, pos, actionId);
                }
				
                if (!getActionItem(pos).isSticky()) {  
                	mDidAction = true;
                	
                    dismiss();
                }
			}
		});
		
		// Since I removed focusable from the popup window,
		// I need to control the selection drawable here
		container.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					
					v.setBackgroundResource(R.drawable.action_item_selected);
				}
				else if(event.getAction() == MotionEvent.ACTION_UP
						|| event.getAction() == MotionEvent.ACTION_CANCEL
						|| event.getAction() == MotionEvent.ACTION_OUTSIDE){
					v.setBackgroundResource(android.R.color.transparent);
				}
				
				
				
				return false;
			}
			
		});
		
		container.setFocusable(true);
		container.setClickable(true);
	
			 
		if (mOrientation == HORIZONTAL && mChildPos != 0) {
            View separator = mInflater.inflate(R.layout.horiz_separator, null);
            
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
            
            separator.setLayoutParams(params);
            separator.setPadding(5, 0, 5, 0);
            
            mTrack.addView(separator, mInsertPos);
            
            mInsertPos++;
        }
		
		mTrack.addView(container, mInsertPos);
		
		mChildPos++;
		mInsertPos++;
	}
	
	/**
	 * Shows the quick action menu using the given Rect as the anchor.
	 * @param parent
	 * @param rect
	 */
	public void show(View parent, Rect rect, String selectedText){
		
		preShow();
		
		int xPos, yPos, arrowPos;
		
		mDidAction 			= false;
        mSelectedText = selectedText;
        Log.d(TAG, "selected text = " + mSelectedText);
		
		int[] location 		= new int[2];
		parent.getLocationOnScreen(location);
		
		int parentXPos = location[0];
		int parentYPos = location[1];

		Rect anchorRect 	= new Rect(parentXPos + rect.left, parentYPos + rect.top, parentXPos + rect.left + rect.width(), parentYPos + rect.top 
            	+ rect.height());
		int width = anchorRect.width();
		int height = anchorRect.height();
		
		//mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		mRootView.measure(ROOT_VIEW_WIDTH, ROOT_VIEW_HEIGHT);
	
		int rootHeight 		= mRootView.getMeasuredHeight();
		
		if (rootWidth == 0) {
			rootWidth		= mRootView.getMeasuredWidth();
		}
		
		int screenWidth 	= mWindowManager.getDefaultDisplay().getWidth();
		int screenHeight	= mWindowManager.getDefaultDisplay().getHeight();
		
		//automatically get X coord of popup (top left)
		if ((anchorRect.left + parentXPos + rootWidth) > screenWidth) {
			xPos 		= anchorRect.left - (rootWidth-width);			
			xPos 		= (xPos < 0) ? 0 : xPos;
			
			
			arrowPos 	= anchorRect.centerX()-xPos;
			
		} else {
			if (width > rootWidth) {
				xPos = anchorRect.centerX() - (rootWidth/2);
			} else {
				xPos = anchorRect.left;
			}
			
			
			arrowPos = anchorRect.centerX()-xPos;
		}
		
		
		int dyTop			= anchorRect.top;
		int dyBottom		= screenHeight - anchorRect.bottom;

		boolean onTop		= (dyTop > dyBottom) ? true : false;

		if (onTop) {
			if (rootHeight > dyTop) {
				yPos 			= 15;
				LayoutParams l 	= mScroller.getLayoutParams();
				l.height		= dyTop - height;
			} else {
				yPos = anchorRect.top - rootHeight;
			}
		} else {
			yPos = anchorRect.bottom;
			
			if (rootHeight > dyBottom) { 
				LayoutParams l 	= mScroller.getLayoutParams();
				l.height		= dyBottom;
			}
		}
		
		
		//showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up), arrowPos);
		
		// No arrows
		mArrowUp.setVisibility(View.INVISIBLE);
		mArrowDown.setVisibility(View.INVISIBLE);
		
		setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
		
		mWindow.showAtLocation(parent, Gravity.NO_GRAVITY, xPos, yPos);
		
	}
	
	/**
	 * Show quickaction popup. Popup is automatically positioned, on top or bottom of anchor view.
	 * 
	 */
	public void show (View anchor) {
		preShow();
		
		int xPos, yPos, arrowPos;
		
		mDidAction 			= false;
		
		int[] location 		= new int[2];
	
		anchor.getLocationOnScreen(location);

		Rect anchorRect 	= new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] 
		                	+ anchor.getHeight());

		//mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		mRootView.measure(ROOT_VIEW_WIDTH, ROOT_VIEW_HEIGHT);
	
		int rootHeight 		= mRootView.getMeasuredHeight();
		
		if (rootWidth == 0) {
			rootWidth		= mRootView.getMeasuredWidth();
		}
		
		int screenWidth 	= mWindowManager.getDefaultDisplay().getWidth();
		int screenHeight	= mWindowManager.getDefaultDisplay().getHeight();
		
		//automatically get X coord of popup (top left)
		if ((anchorRect.left + rootWidth) > screenWidth) {
			xPos 		= anchorRect.left - (rootWidth-anchor.getWidth());			
			xPos 		= (xPos < 0) ? 0 : xPos;
			
			arrowPos 	= anchorRect.centerX()-xPos;
			
		} else {
			if (anchor.getWidth() > rootWidth) {
				xPos = anchorRect.centerX() - (rootWidth/2);
			} else {
				xPos = anchorRect.left;
			}
			
			arrowPos = anchorRect.centerX()-xPos;
		}
		
		int dyTop			= anchorRect.top;
		int dyBottom		= screenHeight - anchorRect.bottom;

		boolean onTop		= (dyTop > dyBottom) ? true : false;

		if (onTop) {
			if (rootHeight > dyTop) {
				yPos 			= 15;
				LayoutParams l 	= mScroller.getLayoutParams();
				l.height		= dyTop - anchor.getHeight();
			} else {
				yPos = anchorRect.top - rootHeight;
			}
		} else {
			yPos = anchorRect.bottom;
			
			if (rootHeight > dyBottom) { 
				LayoutParams l 	= mScroller.getLayoutParams();
				l.height		= dyBottom;
			}
		}
		
		showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up), arrowPos);
		
		setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
		
		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}
	
	/**
	 * Set animation style
	 * 
	 * @param screenWidth screen width
	 * @param requestedX distance from left edge
	 * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top of anchor view
	 * 		  and vice versa
	 */
	private void setAnimationStyle(int screenWidth, int requestedX, boolean onTop) {
		int arrowPos = requestedX - mArrowUp.getMeasuredWidth()/2;

		switch (mAnimStyle) {
		case ANIM_GROW_FROM_LEFT:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
			break;
					
		case ANIM_GROW_FROM_RIGHT:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
			break;
					
		case ANIM_GROW_FROM_CENTER:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
		break;
			
		case ANIM_REFLECT:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect : R.style.Animations_PopDownMenu_Reflect);
		break;
		
		case ANIM_AUTO:
			if (arrowPos <= screenWidth/4) {
				mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
			} else if (arrowPos > screenWidth/4 && arrowPos < 3 * (screenWidth/4)) {
				mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
			} else {
				mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
			}
					
			break;
		}
	}
	
	/**
	 * Show arrow
	 * 
	 * @param whichArrow arrow type resource id
	 * @param requestedX distance from left screen
	 */
	private void showArrow(int whichArrow, int requestedX) {
        final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
        final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;

        final int arrowWidth = mArrowUp.getMeasuredWidth();

        showArrow.setVisibility(View.VISIBLE);
        
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams)showArrow.getLayoutParams();
       
        param.leftMargin = requestedX - arrowWidth / 2;
        
        hideArrow.setVisibility(View.INVISIBLE);
    }
	
	/**
	 * Set listener for window dismissed. This listener will only be fired if the quicakction dialog is dismissed
	 * by clicking outside the dialog or clicking on sticky item.
	 */
	public void setOnDismissListener(QuickAction.OnDismissListener listener) {
		setOnDismissListener(this);
		
		mDismissListener = listener;
	}
	
	@Override
	public void onDismiss() {
		if (!mDidAction && mDismissListener != null) {
			mDismissListener.onDismiss();
            //TBD:: find a better place for remove
            FileMasterController.getInstance().removeFileStatusUpdateListener(this, FileConfiguration.FILE_ID);
		}
	}

    /**
	 * Listener for item click
	 *
	 */
	public interface OnActionItemClickListener {
		public abstract void onItemClick(QuickAction source, int pos, int actionId);
	}
	
	/**
	 * Listener for window dismiss
	 * 
	 */
	public interface OnDismissListener {
		public abstract void onDismiss();
	}

    /**
     * Set detail view to either dictionary meaning or dictionary download status
     * depending on the state
     */
    public void setDetailView() {
        if(FileMasterController.getInstance().isFileAvailable(FileConfiguration.FILE_ID)){
            setDetailViewToDictionaryMeaning();
        } else{
            setDetailViewToDownloadStatusInformation();
            FileMasterController.getInstance().addFileStatusUpdateListener(this, FileConfiguration.FILE_ID);
            //Look into callback method: onStatusUpdate()
        }
    }

    private void setDetailViewToDictionaryMeaning() {
        //FIXME
        if(mSelectedText != null){

            DictionaryEntry  dictionaryEntry = EnglishDictionaryDatabaseManager.getEntry(mSelectedText);
        }
    }

    private void setDetailViewToDownloadStatusInformation() {

        downloadStatusView.setVisibility(View.VISIBLE);
        dictionaryMeaningView.setVisibility(View.GONE);
    }


    @Override
    public void onStatusUpdate(final FileStatus fileStatus) {

        ViewGroup statusInfoLayout = (ViewGroup) downloadStatusView.findViewById(R.id.popup_download_status_layout_root);

        //update the UI

        TextView title = (TextView) statusInfoLayout.findViewById(R.id.popup_download_status_title);
        TextView statusInfo = (TextView)statusInfoLayout.findViewById(R.id.popup_download_status_information);
        SeekBar progressBar = (SeekBar)statusInfoLayout.findViewById(R.id.popup_download_status_progress_bar);
        Button downloadButton = (Button)statusInfoLayout.findViewById(R.id.popup_download_status_download_button);

        // default
        progressBar.setVisibility(View.GONE);
        downloadButton.setVisibility(View.GONE);

        switch(fileStatus.getFileState()){

            case NOT_FOUND:
                title.setText("Download Dictionary");
                statusInfo.setText("Quickly see word meaning without need of internet");
                downloadButton.setVisibility(View.VISIBLE);
                break;

            case WITH_DOWNLOAD_MANAGER:
                title.setText(fileStatus.getFileStatusSubject());
                statusInfo.setText(fileStatus.getFileStatusInfo());
                if(fileStatus.getDownloadManagerStatus()== DownloadManager.STATUS_RUNNING){
                    progressBar.setVisibility(View.VISIBLE);
                    final double progressPercent = fileStatus.getProgressPercent();
                    progressBar.setProgress((int)progressPercent);
                }
                break;

            case DOWNLOAD_FAILED:
                title.setText("Download Failed");
                statusInfo.setText("Last attempt to download failed. Try again!");
                downloadButton.setVisibility(View.VISIBLE);
                break;

            case DOWNLOAD_COMPLETE:
                title.setText("Download Complete");
                statusInfo.setText("Dictionary is downloaded. Preparing the dictionary...");
                break;

            case UNZIPPING:
                title.setText("Preparing Dictionary");
                statusInfo.setText("Please wait a min while preparing the dictionary to use...");
                break;

            case AVAILABLE:
                setDetailViewToDictionaryMeaning();
                FileMasterController.getInstance().removeFileStatusUpdateListener(this, FileConfiguration.FILE_ID);
                break;

            case FAILED_OTHER_REASON:
                title.setText("FAILURE");
                statusInfo.setText("Preparing dictionary for offline use has failed for some unknown reason. Try downloading again!");
                downloadButton.setVisibility(View.VISIBLE);
                break;

            default:
                FileMasterController.getInstance().removeFileStatusUpdateListener(this, FileConfiguration.FILE_ID);
                Log.e(TAG, "File in Unknown State");
        }
    }

    private OnClickListener downloadButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
           FileMasterController.getInstance().requestDownload(FileConfiguration.FILE_ID);

        }
    };




}