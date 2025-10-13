#!/usr/bin/env python
"""
CSV to SQLite Converter

This script converts CSV files to a SQLite database, reducing file size
and optimizing for demo purposes.

Usage:
    python csv_to_sqlite.py --input-dir input/csv --output-db output/sql/amazon_ecommerce_products.sqlite
"""

import argparse
import os
import glob
import sqlite3
import pandas as pd
import logging
import time

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('csv_to_sqlite')

def convert_csv_to_sqlite(input_dir, output_db, optimize=True):
    """
    Convert all CSV files in input_dir to tables in an SQLite database.
    
    Args:
        input_dir: Directory containing CSV files
        output_db: Path to output SQLite database
        optimize: Whether to optimize the database after conversion
    
    Returns:
        tuple: (original_size_mb, new_size_mb, compression_ratio)
    """
    # Create output directory if it doesn't exist
    os.makedirs(os.path.dirname(output_db), exist_ok=True)
    
    # Create database connection
    conn = sqlite3.connect(output_db)
    logger.info(f"Created SQLite database: {output_db}")
    
    # Find all CSV files
    csv_pattern = os.path.join(input_dir, "*.csv")
    csv_files = glob.glob(csv_pattern)
    
    if not csv_files:
        logger.error(f"No CSV files found in {input_dir}")
        return None
    
    logger.info(f"Found {len(csv_files)} CSV files to convert")
    
    # Track total size of original CSV files
    total_csv_size = 0
    start_time = time.time()
    
    # Process each CSV file
    for csv_file in csv_files:
        file_size = os.path.getsize(csv_file) / (1024 * 1024)  # Size in MB
        total_csv_size += file_size
        
        # Get table name from file name
        table_name = os.path.splitext(os.path.basename(csv_file))[0]
        # Replace any invalid characters for SQLite table names
        table_name = ''.join(c if c.isalnum() or c == '_' else '_' for c in table_name)
        
        logger.info(f"Converting {csv_file} ({file_size:.2f} MB) to table '{table_name}'")
        
        try:
            # Read CSV in chunks to handle large files
            for chunk_num, chunk in enumerate(pd.read_csv(csv_file, chunksize=100000)):
                if chunk_num == 0:
                    # First chunk, create table
                    chunk.to_sql(table_name, conn, if_exists='replace', index=False)
                    # Create indexes on likely key columns
                    for col in chunk.columns:
                        if any(key_name in col.lower() for key_name in ['id', 'key', 'code']):
                            index_name = f"idx_{table_name}_{col}"
                            conn.execute(f"CREATE INDEX IF NOT EXISTS {index_name} ON {table_name} ({col})")
                else:
                    # Append to existing table
                    chunk.to_sql(table_name, conn, if_exists='append', index=False)
                
                if chunk_num % 10 == 0:
                    logger.info(f"  Processed {chunk_num+1} chunks ({(chunk_num+1)*100000} rows)")
            
            logger.info(f"Successfully imported table '{table_name}'")
        except Exception as e:
            logger.error(f"Error converting {csv_file}: {e}")
    
    # Optimize the database if requested
    if optimize:
        logger.info("Optimizing database...")
        conn.execute("PRAGMA analysis_limit=1000")
        conn.execute("PRAGMA optimize")
        conn.execute("VACUUM")
    
    conn.commit()
    conn.close()
    
    elapsed_time = time.time() - start_time
    
    # Calculate final database size and compression ratio
    db_size = os.path.getsize(output_db) / (1024 * 1024)  # Size in MB
    compression_ratio = total_csv_size / db_size if db_size > 0 else 0
    
    logger.info(f"Conversion complete in {elapsed_time:.2f} seconds!")
    logger.info(f"Original CSV size: {total_csv_size:.2f} MB")
    logger.info(f"SQLite database size: {db_size:.2f} MB")
    logger.info(f"Compression ratio: {compression_ratio:.2f}x")
    
    return (total_csv_size, db_size, compression_ratio)

def main():
    parser = argparse.ArgumentParser(description='Convert CSV files to SQLite database')
    parser.add_argument('--input-dir', default='input/csv', help='Directory containing CSV files')
    parser.add_argument('--output-db', default='output/sql/amazon_ecommerce_products.sqlite', help='Output SQLite database file')
    parser.add_argument('--no-optimize', action='store_true', help='Skip optimization step')
    
    args = parser.parse_args()
    
    convert_csv_to_sqlite(args.input_dir, args.output_db, optimize=not args.no_optimize)

if __name__ == "__main__":
    main()

