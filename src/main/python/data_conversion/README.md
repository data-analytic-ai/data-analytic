# CSV to SQLite Converter - Guide

## What This Tool Does

This tool converts CSV files into a SQLite database, which offers several advantages:

- **Reduced file size**: Typically 3-5x smaller than the original CSVs
- **Faster querying**: SQLite allows SQL queries for efficient data retrieval
- **Preserved relationships**: Maintains connections between related data tables
- **Portability**: Single file database that requires no server

When you convert the Amazon product dataset:
- Both CSV files (amazon_categories.csv and amazon_products.csv) will be combined into one SQLite database file
- The database will contain two tables, one for each CSV
- The relationship between products and categories is preserved

## Directory Structure

```
src/main/
├── java/                        # Java source code
├── python/                      # Python source code
│   └── data_conversion/         # Data conversion tools
│       ├── input/csv/           # Put your CSV files here
│       ├── output/sql/          # SQLite files location
│       ├── __init__.py          # Package initialization
│       ├── csv_to_sqlite.py     # Conversion script
│       └── README.md            # This documentation
└── resources/                   # Shared resources
```

## Setup and Usage

### Prerequisites
1. Install Python from [python.org](https://www.python.org/downloads/) or Microsoft Store
2. Install the required package:
   ```
   pip install pandas
   ```

### Running the Conversion
Navigate to the script directory:
```
$ cd src/main/python/data_conversion
```

Run the script:
```
python csv_to_sqlite.py
```

The script will automatically:
1. Read all CSV files from the input/csv directory
2. Create a database file at output/sql/amazon_ecommerce_products.sqlite
3. Process each CSV into its own table
4. Create indexes for better performance
5. Show progress and final statistics

### Custom Paths (Optional)
If you need to use different locations:
```
python csv_to_sqlite.py --input-dir custom/path --output-db custom/output.sqlite
```

## How It Works

The conversion process:
1. Creates a new SQLite database file (creating any necessary folders)
2. Reads each CSV file in manageable chunks (for large files)
3. Creates a table for each CSV file with matching structure
4. Automatically adds indexes on ID fields for faster queries
5. Optimizes the database for size and performance

After conversion, you'll have a single SQLite file containing multiple tables - one for each CSV file. The data relationships are preserved, allowing you to perform joins and complex queries across tables.

## Troubleshooting

- **"Python not found"**: Ensure Python is installed and in your PATH
- **"No module named pandas"**: Run `pip install pandas`
- **"No CSV files found"**: Verify files are in the input/csv directory
- **Permission errors**: Run command prompt as Administrator

You can safely remove the original CSV files after successful conversion as all data will be stored in the SQLite database.

