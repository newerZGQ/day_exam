#!/bin/bash

TAG_NAME=$1

if [ -z "$TAG_NAME" ]; then
  echo "Usage: $0 <tag_name>"
  exit 1
fi

echo "Processing tag: $TAG_NAME"

# Delete local tag
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "Deleting local tag '$TAG_NAME'..."
    git tag -d "$TAG_NAME"
else
    echo "Local tag '$TAG_NAME' does not exist."
fi

# Delete remote tag
if git ls-remote --exit-code --tags origin "$TAG_NAME" >/dev/null 2>&1; then
    echo "Deleting remote tag '$TAG_NAME'..."
    git push origin :refs/tags/"$TAG_NAME"
else
    echo "Remote tag '$TAG_NAME' does not exist."
fi

# Create and push new tag
echo "Creating new tag '$TAG_NAME'..."
git tag "$TAG_NAME"

echo "Pushing tag '$TAG_NAME' to origin..."
git push origin "$TAG_NAME"

echo "Tag '$TAG_NAME' successfully created and pushed."
