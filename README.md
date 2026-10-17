# TODO-my-package Project

First, choose a package name, e.g. `mypack` and then update the package name via `find . -type f -exec sed -i 's/TODO-my-package/mypack/g' {} \;` and rename `src/TODO-my-package` to `src/mypack`.
Afterwards, find all remaining `TODO` and update the sections via `grep -r TODO .` .


## Setup

Install [pipx](https://pipx.pypa.io/stable/installation/) and [pyenv](https://github.com/pyenv/pyenv?tab=readme-ov-file#installation) and then run the following commands:

```bash
# Install python 3.12 with pyenv
pyenv install 3.12.3
pyenv local 3.12.3
# Verify that correct Python is used
python --version
# Install pre-commit
pipx install pre-commit
# Setup pre-commit for this repository
pre-commit install
# Install hatch
pipx install hatch
# Create a virtual environment using hatch-pip-compile which is configured to create uv-locked requirements-files and then install these dependencies
hatch [-e default] shell
```

Any `hatch run` command will then use a uv-locked dependencies file for creating the virtual environment and then running the command in this environment. 

## Run linter, formatter & tests

```bash
# Run linting and formatter
pre-commit run --all-files
# Run tests either via pytest or via hatch
# Note: This runs tests in the hatch-test environment
hatch test
# Note: This runs tests in the test environment defined in pyproject.toml
hatch run -e test pytest
# OR:
hatch -e test shell
pytest
```

## Run the main-program

```bash
main-cli 
# OR:
hatch run main-cli
```

# Development

## Lock dependencies

Update the dependency in `pyproject.toml` and then run:
```bash
# Locks dependencies with uv by hatch-pip-compile, if this does not update the shell then see section below
hatch shell
# This is run in the background by hatch-pip-compile:
# uv pip compile pyproject.toml -o requirements.txt 
# uv pip sync requirements.txt
```


## Update pyproject.toml
Note that updating the cli-scripts / entry points currently requires that the hatch-environment needs to recreated (see [this](https://github.com/pypa/hatch/issues/771) issue):
```bash
hatch env prune
hatch shell
```
Alternatively, you can use `pip install -e . --no-deps` to reinstall in editable mode after a change. 
