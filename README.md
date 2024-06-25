# Kleber

A simple discord bot for personal usage.

## Installation

* requires Java 21 or later

Run the following command in the source directory:

```bash
./mvnw clean package
```

In the project's target directory you'll see the generated `.zip` file which is named like: `kleber-1.0-SNAPSHOT.zip`.
Extract it to the location of your choice.

## Usage

In the extracted folder, open the `.env` file and fill in required information.

| Key           | Description                                                 |
|---------------|-------------------------------------------------------------|
| LOG_LEVEL     | The level of information to be displayed                    |
| TOKEN         | The token of the account that you would like to login with. |
| ACTIVITY      | Represents a discord activity                               |
| CLIENT_ID     | The id of your spotify application                          |
| CLIENT_SECRET | The secret of your spotify application                      |

Run one of the scripts, based on your OS.

On linux:

```bash
./run.sh
```

Or on windows:

```cmd
run.cmd
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.