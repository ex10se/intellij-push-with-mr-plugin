# IntelliJ GitLab Push with MR creation Plugin

This plugin adds a button that automatically adds merge request options when pushing to GitLab repositories.

## Features

- **Automatic Merge Request Creation**: Each push automatically creates a merge request
- **Dynamic Titles**: Uses the current changelist/task name as the merge request title
- **Hotfix Support**: Automatically sets target branch to `master` for branches containing "hotfix/"
- **Non-stop Work**: Automatically opens the MR url in your default browser 

## How it works

When you use the "Commit-Push-MR" button in the Commit window, the plugin automatically adds the following Git push options:

- `-o merge_request.create` - creates a merge request
- `-o merge_request.title="<changelist_name>"` - uses the changelist name as the title
- `-o merge_request.target=master` - sets the target to master (only for hotfix branches)
