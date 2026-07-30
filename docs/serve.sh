#!/bin/bash
# Simple script to serve the documentation locally

echo "🚀 Starting local documentation server..."
echo ""
echo "📚 Documentation will be available at:"
echo "   http://localhost:8000"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Check if Python 3 is available
if command -v python3 &> /dev/null; then
    cd "$(dirname "$0")"
    python3 -m http.server 8000
elif command -v python &> /dev/null; then
    cd "$(dirname "$0")"
    python -m SimpleHTTPServer 8000
else
    echo "❌ Python not found. Please install Python 3 or use one of these alternatives:"
    echo ""
    echo "Option 1: Using Node.js (if installed):"
    echo "   npx http-server -p 8000"
    echo ""
    echo "Option 2: Using PHP (if installed):"
    echo "   php -S localhost:8000"
    echo ""
    echo "Option 3: Open index.html directly in your browser"
    exit 1
fi





