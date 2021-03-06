package gr.tsagi.jekyllforandroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@SuppressLint({ "DefaultLocale", "SimpleDateFormat" })
public class NewPostActivity extends Activity {
    String mUsername;
    String mToken;
    String mDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    String mTitle;
    String mCategory;
    String mTags;
    String mContent;

    private View mNewPostFormView;
    private View mNewPostStatusView;
    @SuppressWarnings("unused")
	private TextView mNewPostStatusMessageView;

    private SharedPreferences settings;

    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        
        /**
         * Publish and Preview Buttons disappear when Typing to give screen space
         */

        mNewPostFormView = findViewById(R.id.newpost_form);
        mNewPostStatusView = findViewById(R.id.newpost_status);
        mNewPostStatusMessageView = (TextView) findViewById(R.id.newpost_status_message);

        /**
         * Restore draft if any is available
         */
        
        restorePreferences();
        setStrings();

        if(mToken == ""){
            Toast.makeText(NewPostActivity.this, "Please login", Toast.LENGTH_LONG ).show();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_draft:
                clearDraft();
                return true;
            case R.id.action_publish:
            	publishPost();
            	return true;
            case R.id.action_preview:
            	previewMarkdown();
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    private void clearDraft(){
        mTitle    = "";
        mCategory = "";
        mTags     = "";
        mContent  = "";

        setStrings();
    }

    private void restorePreferences(){
        settings = getSharedPreferences(
                "gr.tsagi.jekyllforandroid", Context.MODE_PRIVATE);
        mUsername = settings.getString("user_login", "");
        mToken = settings.getString("user_status", "");

        mTitle = settings.getString("draft_title", "");
        mCategory = settings.getString("draft_category", "");
        mTags = settings.getString("draft_tags", "");
        mContent = settings.getString("draft_content", "");
    }
    
    private void publishPost(){
    	
    	final EditText content = (EditText)findViewById(R.id.editTextContent);
    	
    	if(content.getText().toString().isEmpty())
    		Toast.makeText(getApplicationContext(), R.string.newpost_empty, Toast.LENGTH_LONG).show();
    	else {
    	
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
    		savePreferences();
    		builder.setMessage(R.string.dialog_confirm_update);
        
    		// Add the buttons
    		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   /**
    	        	    * Publish post
    	        	    */
    	        	   new UpdateFile().execute();
    	           }
    	       });
    		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	               // User cancelled the dialog
    	           }
    	       });

    		// Create the AlertDialog
    		AlertDialog dialog = builder.create();
    		dialog.show();
    	}
    }

    private  void savePreferences(){
        EditText titleT = (EditText)findViewById(R.id.editTextTitle);
        EditText contentT = (EditText)findViewById(R.id.editTextContent);
        EditText categoryT = (EditText)findViewById(R.id.editTextCategory);
        EditText tagsT = (EditText)findViewById(R.id.editTextTags);

        mTitle = titleT.getText().toString();
        mCategory = categoryT.getText().toString();
        mTags = tagsT.getText().toString();
        mContent = contentT.getText().toString();

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("draft_title", mTitle);
        editor.putString("draft_category", mCategory);
        editor.putString("draft_tags", mTags);
        editor.putString("draft_content", mContent);
        editor.commit();
    }

    private void setStrings(){
        EditText titleT = (EditText)findViewById(R.id.editTextTitle);
        EditText contentT = (EditText)findViewById(R.id.editTextContent);
        EditText categoryT = (EditText)findViewById(R.id.editTextCategory);
        EditText tagsT = (EditText)findViewById(R.id.editTextTags);

        titleT.setText(mTitle);
        categoryT.setText(mCategory);
        tagsT.setText(mTags);
        contentT.setText(mContent);

    }

    @Override
    protected void onStop() {
        /**
         * Save draft
         */
        savePreferences();
        super.onStop();
    }

    @Override
    protected void onStart() {
        /**
         * Return to draft if any is available
         */
        restorePreferences();
        setStrings();
        
        super.onStart();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mNewPostStatusView.setVisibility(View.VISIBLE);
            mNewPostStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mNewPostStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mNewPostFormView.setVisibility(View.VISIBLE);
            mNewPostFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mNewPostFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mNewPostStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mNewPostFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    class UpdateFile extends AsyncTask<Void,Void,Void> {

        @Override
        protected void onPreExecute() {
            hideSoftKeyboard(NewPostActivity.this);
            showProgress(true);
        }
        

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                // based on http://swanson.github.com/blog/2011/07/23/digging-around-the-github-api-take-2.html
                // initialize github client
                GitHubClient client = new GitHubClient();
                EditText titleT = (EditText)findViewById(R.id.editTextTitle);
                EditText contentT = (EditText)findViewById(R.id.editTextContent);
                EditText categoryT = (EditText)findViewById(R.id.editTextCategory);
                EditText tagsT = (EditText)findViewById(R.id.editTextTags);


                mTitle = titleT.getText().toString();
                mCategory = categoryT.getText().toString();
                mTags = tagsT.getText().toString();
                mContent = contentT.getText().toString();

                client.setOAuth2Token(mToken);

                // create needed services
                RepositoryService repositoryService = new RepositoryService();
                CommitService commitService = new CommitService(client);
                DataService dataService = new DataService(client);

                // get some sha's from current state in git
                Repository repository =  repositoryService.getRepository(mUsername, mUsername+".github.com");
                String baseCommitSha = repositoryService.getBranches(repository).get(0).getCommit().getSha();
                RepositoryCommit baseCommit = commitService.getCommit(repository, baseCommitSha);
                String treeSha = baseCommit.getSha();
                
                String completeContent = "---\n" +
                        "layout: post\n" +
                        "title: " + '"' + mTitle + '"' + "\n" +
                        "description: "+ '"' + '"'+" \n" +
                        "category: " + mCategory + "\n" +
                        "tags: [" + mTags + "]"+ "\n" +
                        "---\n" +
                        "{% include JB/setup %}\n" +
                         mContent;

                // create new blob with data
                Blob blob = new Blob();
                blob.setContent(completeContent).setEncoding(Blob.ENCODING_UTF8);
                String blob_sha = dataService.createBlob(repository, blob);
                Tree baseTree = dataService.getTree(repository, treeSha);

                // set path
                String path = mDate + "-" + mTitle.toLowerCase().replace(' ', '-')
                        .replace(",","").replace("!","").replace(".","") + ".md";

                // create new tree entry
                TreeEntry treeEntry = new TreeEntry();
                
                // for testing
//                treeEntry.setPath("pages/" + path);
                
                // working
                 treeEntry.setPath("_posts/" + path);
                treeEntry.setMode(TreeEntry.MODE_BLOB);
                treeEntry.setType(TreeEntry.TYPE_BLOB);
                treeEntry.setSha(blob_sha);
                treeEntry.setSize(blob.getContent().length());
                Collection<TreeEntry> entries = new ArrayList<TreeEntry>();
                entries.add(treeEntry);
                Tree newTree = dataService.createTree(repository, entries, baseTree.getSha());

                // create commit
                Commit commit = new Commit();
                commit.setMessage("Post from Android at " + mDate);
                commit.setTree(newTree);
                List<Commit> listOfCommits = new ArrayList<Commit>();
                listOfCommits.add(new Commit().setSha(baseCommitSha));
                // listOfCommits.containsAll(base_commit.getParents());
                commit.setParents(listOfCommits);
                // commit.setSha(base_commit.getSha());
                Commit newCommit = dataService.createCommit(repository, commit);

                // create resource
                TypedResource commitResource = new TypedResource();
                commitResource.setSha(newCommit.getSha());
                commitResource.setType(TypedResource.TYPE_COMMIT);
                commitResource.setUrl(newCommit.getUrl());

                // get master reference and update it
                Reference reference = dataService.getReference(repository, "heads/master");
                reference.setObject(commitResource);
                dataService.editReference(repository, reference, true);

                // success

            } catch (Exception e) {
                // error
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            showProgress(false);
            Toast.makeText(NewPostActivity.this, "Post published!", Toast.LENGTH_LONG ).show();

            /**
             * Clear fields for the next post
             */
            clearDraft();
            setStrings();

            finish();
        }
    }
    
    public void previewMarkdown(){
    	savePreferences();
    	if (!mContent.isEmpty()){
    		Intent myIntent = new Intent(getApplicationContext(), PreviewMarkdownActivity.class);
    		myIntent.putExtra("content", mContent);
    		startActivity(myIntent);
    	}else
    		Toast.makeText(this, "Nothing to preview", Toast.LENGTH_SHORT).show();
    }


}