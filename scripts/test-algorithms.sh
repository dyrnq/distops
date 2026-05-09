#!/usr/bin/env bash

# ============================================================
# Token Algorithm Test Script
# Tests EC, RSA, and HMAC token signing algorithms
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
INST_ID=1
INST_NAME="default"
API_HOST="http://localhost:12680"
API_USER="admin"
API_PASS="admin"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo ""
    echo "============================================================"
    echo " $1"
    echo "============================================================"
    echo ""
}

# Check if running on server
check_environment() {
    log_info "Checking environment..."
    if ! grep -q vagrant <<< $(id); then
        log_error "This script must be run on the vagrant server"
        exit 1
    fi
    log_success "Environment check passed"
}

# Login and get JWT token
login() {
    log_info "Logging in as $API_USER..."

    name=$(echo -n "$API_USER" | base64)
    name=$(echo -n "$name" | base64)

    pass=$(echo -n "$API_PASS" | base64)
    pass=$(echo -n "$pass" | base64)

    LOGIN_RESULT=$(curl -s -X POST "$API_HOST/token/getToken" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$name\",
            \"pass\": \"$pass\"
        }")

    JWT_TOKEN=$(echo "$LOGIN_RESULT" | jq -r '.data')

    if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "null" ]; then
        log_error "Failed to login: $LOGIN_RESULT"
        exit 1
    fi

    log_success "Login successful, JWT token obtained"
}

# Generate key pair for specified algorithm
generate_key() {
    local key_type=$1
    local algorithm=$2

    print_header "Generating $key_type ($algorithm) Key Pair"

    log_info "Generating $key_type key pair with algorithm $algorithm..."

    # Login first
    login

    # Use curl to call the API with JWT token
    RESULT=$(curl -s -X POST "$API_HOST/api/inst/keypair" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -d "{
            \"instId\": $INST_ID,
            \"keyType\": \"$key_type\",
            \"algorithm\": \"$algorithm\"
        }")

    echo "$RESULT" | jq '.'

    # Check if result contains success
    if echo "$RESULT" | jq -e '.code == 200' > /dev/null 2>&1; then
        log_success "Key pair generated successfully"
    else
        log_error "Failed to generate key pair: $RESULT"
        exit 1
    fi
}

# Rebuild application
rebuild() {
    print_header "Rebuilding Application"

    log_info "Building jar package..."
    cd /vagrant
    ./build.sh

    if [ $? -eq 0 ]; then
        log_success "Build successful"
    else
        log_error "Build failed"
        exit 1
    fi

    log_info "Building docker image..."
    ./buildx.sh

    if [ $? -eq 0 ]; then
        log_success "Docker image built successfully"
    else
        log_error "Docker build failed"
        exit 1
    fi
}

# Restart container
restart() {
    print_header "Restarting Container"

    log_info "Stopping old container..."
    cd /vagrant && ./run.sh

    log_info "Waiting for service to start..."
    sleep 8

    # Check if service is running
    if docker ps | grep -q distops; then
        log_success "Container started successfully"
    else
        log_error "Container failed to start"
        exit 1
    fi

    # Check logs for initialization
    log_info "Checking service initialization..."
    docker logs distops 2>&1 | grep -i "token service initialized" | tail -3
}

# Run test
run_test() {
    local test_name=$1
    
    print_header "Running Test: $test_name"
    
    log_info "Running test-pull-push.sh..."
    bash /vagrant/scripts/test-pull-push.sh
    
    if [ $? -eq 0 ]; then
        log_success "Test completed successfully"
    else
        log_error "Test failed"
        exit 1
    fi
    
    # Wait for events to be processed
    log_info "Waiting for events to be processed..."
    sleep 5
    
    # Check database
    log_info "Checking database..."
    mysql -h 127.0.0.1 -P 13306 -u root -p666666 distops -e "
        SELECT
            repo_name,
            tag_name,
            media_type,
            os_arch,
            manifest_last_pushed
        FROM artifact_manifest_view
        ORDER BY manifest_last_pushed DESC
        LIMIT 5;
    " 2>&1 | grep -v "Warning"
}

# Check token type in logs
check_token_type() {
    print_header "Checking Token Type in Logs"
    
    log_info "Recent token issuance logs:"
    docker logs distops 2>&1 | grep -i "token issued\|keyType" | tail -5
    
    log_info "Active token service:"
    docker logs distops 2>&1 | grep -i "token service initialized" | tail -1
}

# Main test function
run_algorithm_test() {
    local key_type=$1
    local algorithm=$2
    local test_name="$key_type ($algorithm)"
    
    print_header "Testing $test_name"
    
    # Step 1: Generate key
    generate_key "$key_type" "$algorithm"
    
    # Step 2: Rebuild
    rebuild
    
    # Step 3: Restart
    restart
    
    # Step 4: Check token type
    check_token_type
    
    # Step 5: Run test
    run_test "$test_name"
    
    log_success "========================================="
    log_success "Test $test_name COMPLETED"
    log_success "========================================="
}

# Show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  Quick test options:"
    echo "    --ec          Test EC with ES256 (default)"
    echo "    --rsa         Test RSA with RS256 (default)"
    echo "    --hmac        Test HMAC with HS256 (default)"
    echo "    --all         Test all default algorithms"
    echo ""
    echo "  Custom test options:"
    echo "    --key-type TYPE   Specify key type: EC, RSA, or HMAC"
    echo "    --algorithm ALG   Specify algorithm: ES256/ES384/ES512, RS256/RS384/RS512, HS256/HS384/HS512"
    echo ""
    echo "  Other options:"
    echo "    --check       Check current token service status only"
    echo "    --help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --ec                    # Test EC with ES256"
    echo "  $0 --key-type EC --algorithm ES384   # Test EC with ES384"
    echo "  $0 --key-type EC --algorithm ES512   # Test EC with ES512"
    echo "  $0 --key-type RSA --algorithm RS384  # Test RSA with RS384"
    echo "  $0 --key-type HMAC --algorithm HS512 # Test HMAC with HS512"
    echo "  $0 --all                   # Test all default algorithms"
    echo "  $0 --check                 # Check current status"
    echo ""
}

# Check current token type
check_current() {
    print_header "Current Token Service Status"
    
    log_info "Container status:"
    docker ps | grep distops
    
    log_info "Token service initialization:"
    docker logs distops 2>&1 | grep -i "token service initialized" | tail -3
    
    log_info "Recent token issuances:"
    docker logs distops 2>&1 | grep -i "token issued" | tail -5
}

# Parse arguments
if [ $# -eq 0 ]; then
    show_usage
    exit 1
fi

check_environment

# Variables for custom options
KEY_TYPE=""
ALGORITHM=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --ec)
            run_algorithm_test "EC" "ES256"
            shift
            ;;
        --rsa)
            run_algorithm_test "RSA" "RS256"
            shift
            ;;
        --hmac)
            run_algorithm_test "HMAC" "HS256"
            shift
            ;;
        --all)
            run_algorithm_test "EC" "ES256"
            run_algorithm_test "RSA" "RS256"
            run_algorithm_test "HMAC" "HS256"
            shift
            ;;
        --check)
            check_current
            shift
            ;;
        --key-type)
            KEY_TYPE="$2"
            shift 2
            ;;
        --algorithm)
            ALGORITHM="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# If custom key-type and algorithm are specified, run the test
if [ -n "$KEY_TYPE" ]; then
    # Set default algorithm if not specified
    if [ -z "$ALGORITHM" ]; then
        case "$KEY_TYPE" in
            EC|ec)
                ALGORITHM="ES256"
                ;;
            RSA|rsa)
                ALGORITHM="RS256"
                ;;
            HMAC|hmac)
                ALGORITHM="HS256"
                ;;
            *)
                ALGORITHM="ES256"
                ;;
        esac
    fi
    run_algorithm_test "$KEY_TYPE" "$ALGORITHM"
fi

log_success "All tests completed!"
