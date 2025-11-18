# Git Custom Push Plugin

This plugin automatically adds merge request options when pushing to Git repositories.

## Features

- **Automatic Merge Request Creation**: Every push automatically creates a merge request
- **Dynamic Titles**: Uses the current changelist/task name as the merge request title
- **Hotfix Support**: Automatically sets target branch to `master` for branches containing "hotfix/"

## How it works

When you use the built-in "Commit and Push..." button, the plugin automatically adds these Git push options:

- `-o merge_request.create` - Creates a merge request
- `-o merge_request.title="<changelist_name>"` - Uses changelist name as title
- `-o merge_request.target=master` - Sets target to master (only for hotfix branches)
