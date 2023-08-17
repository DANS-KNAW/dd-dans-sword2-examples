EXAMPLE_ZIP=$1
PARENT_DIR=$(dirname $EXAMPLE_ZIP)

pushd $PARENT_DIR || exit 1
unzip $EXAMPLE_ZIP
popd || exit 1

