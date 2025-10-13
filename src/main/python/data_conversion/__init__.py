"""
Data Conversion Utilities

This package provides tools for converting between different data formats
used in the Data Analytic project.
"""

__version__ = '0.1.0'
__author__ = 'Data Analytics Team'

# Import key functions to make them available at package level
from .csv_to_sqlite import convert_csv_to_sqlite

# Define what gets imported with "from data_conversion import *"
__all__ = ['convert_csv_to_sqlite']

