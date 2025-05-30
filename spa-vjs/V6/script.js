document.addEventListener('DOMContentLoaded', () => {
  // --- Configuration ---
  const API_BASE_URL = 'http://localhost:8080/api'; // Your Spring Boot backend URL
  const PREVIEW_ROW_LIMIT = 10;

  // --- DOM Elements ---
  const schemaFieldsContainer = document.getElementById('schemaFieldsContainer');
  const addFieldBtn = document.getElementById('addFieldBtn');
  const schemaFieldTemplate = document.getElementById('schemaFieldTemplate');
  const rowCountInput = document.getElementById('rowCount');
  const formatSelect = document.getElementById('formatSelect');
  const sqlOptionsDiv = document.getElementById('sqlOptions');
  const sqlTableNameInput = document.getElementById('sqlTableName');
  const generateBtn = document.getElementById('generateBtn');
  const previewTable = document.getElementById('previewTable');
  const previewTableHead = previewTable.querySelector('thead');
  const previewTableBody = previewTable.querySelector('tbody');
  const previewPlaceholder = document.getElementById('previewPlaceholder');
  const previewLoadingIndicator = document.getElementById('previewLoadingIndicator');
  const loadingIndicator = document.getElementById('loadingIndicator'); // General loading
  const errorDisplay = document.getElementById('errorDisplay');
  const darkModeToggle = document.getElementById('darkModeToggle');
  // Schema Management Elements
  const schemaNameInput = document.getElementById('schemaName');
  const saveSchemaBtn = document.getElementById('saveSchemaBtn');
  const loadSchemaSelect = document.getElementById('loadSchemaSelect');
  const loadSchemaBtn = document.getElementById('loadSchemaBtn');
  const deleteSchemaBtn = document.getElementById('deleteSchemaBtn');
  const shareLinkDisplay = document.getElementById('shareLinkDisplay');
  const shareLinkAnchor = document.getElementById('shareLinkAnchor');
  const apiLinkSpan = document.getElementById('apiLinkSpan');


  let availableDataTypes = {}; // Store fetched data types { category: [ {key, name, category}, ... ] }
  let isLoadingDataTypes = false;

  // --- API Helper Functions ---

  async function apiFetch(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const defaultOptions = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        // Add other headers like Authorization if needed later
      },
    };
    const mergedOptions = { ...defaultOptions, ...options };

    // Stringify body if it's an object
    if (mergedOptions.body && typeof mergedOptions.body === 'object') {
      mergedOptions.body = JSON.stringify(mergedOptions.body);
    }

    try {
      const response = await fetch(url, mergedOptions);

      // Handle non-JSON responses for file download
      const contentType = response.headers.get('content-type');
      if (options.expectBlob || (contentType && !contentType.includes('application/json') && response.ok && options.method?.toUpperCase() !== 'DELETE')) { // DELETE might return 204
        if (!response.ok) {
          let errorText = `HTTP error ${response.status}`;
          try { errorText = await response.text(); } catch(e){} // Try to get text error
          throw new Error(errorText || `HTTP error ${response.status}`);
        }
        const blob = await response.blob();
        const contentDisposition = response.headers.get('content-disposition');
        let filename = 'downloaded_file';
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i);
          if (filenameMatch && filenameMatch.length > 1) {
            filename = filenameMatch[1];
          }
        }
        return { blob, filename }; // Return blob and filename for downloads
      }

      // Handle potential empty body for 204 No Content (like DELETE)
      if (response.status === 204) {
        return null; // Or return an indicator object like { success: true }
      }

      // For JSON responses
      const data = await response.json();
      if (!response.ok) {
        // Attempt to parse backend error message if available
        const errorMessage = data?.message || data?.error || `HTTP error ${response.status}`;
        throw new Error(errorMessage);
      }
      return data;

    } catch (error) {
      console.error(`API Fetch Error (${options.method || 'GET'} ${endpoint}):`, error);
      throw error; // Re-throw to be caught by calling function
    }
  }


  // --- UI Helper Functions ---

  function displayError(message) {
    errorDisplay.textContent = `Error: ${message}`;
    errorDisplay.classList.remove('hidden');
  }

  function clearError() {
    errorDisplay.textContent = '';
    errorDisplay.classList.add('hidden');
  }

  function setGlobalLoading(isLoading, message = 'Contacting Backend...') {
    loadingIndicator.textContent = message;
    if (isLoading) {
      loadingIndicator.classList.remove('hidden');
      generateBtn.disabled = true; // Disable main generate button
      saveSchemaBtn.disabled = true;
      loadSchemaBtn.disabled = true;
      deleteSchemaBtn.disabled = true;
    } else {
      loadingIndicator.classList.add('hidden');
      generateBtn.disabled = false;
      saveSchemaBtn.disabled = false;
      loadSchemaBtn.disabled = false;
      deleteSchemaBtn.disabled = false;
    }
  }

  function setPreviewLoading(isLoading) {
    if(isLoading) {
      previewLoadingIndicator.classList.remove('hidden');
      previewTable.classList.add('hidden');
      previewPlaceholder.classList.add('hidden');
    } else {
      previewLoadingIndicator.classList.add('hidden');
      // Visibility of table/placeholder handled by updatePreviewTable
    }
  }


  function populateDataTypeSelect(selectElement) {
    if (isLoadingDataTypes) {
      selectElement.innerHTML = '<option value="">-- Loading Types... --</option>';
      selectElement.disabled = true;
      return;
    }
    if (!availableDataTypes || Object.keys(availableDataTypes).length === 0) {
      selectElement.innerHTML = '<option value="">-- Error Loading Types --</option>';
      selectElement.disabled = true;
      return;
    }

    selectElement.innerHTML = '<option value="">-- Select Type --</option>';
    selectElement.disabled = false;

    // Sort categories alphabetically
    const sortedCategories = Object.keys(availableDataTypes).sort();

    for (const category of sortedCategories) {
      const optgroup = document.createElement('optgroup');
      optgroup.label = category;
      // Sort types within category alphabetically by name
      const sortedTypes = availableDataTypes[category].sort((a, b) => a.name.localeCompare(b.name));

      sortedTypes.forEach(type => {
        const option = document.createElement('option');
        option.value = type.key;
        option.textContent = type.name;
        optgroup.appendChild(option);
      });
      selectElement.appendChild(optgroup);
    }
  }

  function addSchemaField(fieldData = { name: '', dataType: '', options: '' }) {
    const templateContent = schemaFieldTemplate.content.cloneNode(true);
    const fieldRow = templateContent.querySelector('.schema-field-row');
    const nameInput = fieldRow.querySelector('input[name="fieldName"]');
    const typeSelect = fieldRow.querySelector('select[name="dataType"]');
    const optionsInput = fieldRow.querySelector('input[name="fieldOptions"]');
    const removeBtn = fieldRow.querySelector('.remove-field-btn');

    populateDataTypeSelect(typeSelect); // Populate dropdown

    // Set initial values from fieldData (coming from loaded schema or defaults)
    nameInput.value = fieldData.name || ''; // Use name property from DTO
    typeSelect.value = fieldData.dataType || '';
    optionsInput.value = fieldData.options || '';


    removeBtn.addEventListener('click', () => {
      fieldRow.remove();
      debouncedUpdatePreview(); // Update preview when a field is removed
    });

    // Update preview on change
    nameInput.addEventListener('input', debouncedUpdatePreview);
    typeSelect.addEventListener('change', debouncedUpdatePreview);
    optionsInput.addEventListener('input', debouncedUpdatePreview);

    schemaFieldsContainer.appendChild(fieldRow);
  }

  // Debounce function
  let debounceTimer;
  function debounce(func, delay) {
    return function() {
      const context = this;
      const args = arguments;
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => func.apply(context, args), delay);
    };
  }
  const debouncedUpdatePreview = debounce(updatePreview, 400); // Debounce preview calls

  function getSchemaFromDOM() {
    const fields = [];
    schemaFieldsContainer.querySelectorAll('.schema-field-row').forEach(row => {
      const nameInput = row.querySelector('input[name="fieldName"]');
      const typeSelect = row.querySelector('select[name="dataType"]');
      const optionsInput = row.querySelector('input[name="fieldOptions"]');
      // Field name and type are required
      if (nameInput.value.trim() && typeSelect.value) {
        fields.push({
          name: nameInput.value.trim(),
          dataType: typeSelect.value,
          options: optionsInput.value.trim() || null // Send null if options are empty
        });
      }
    });
    return fields;
  }

  function updatePreviewTable(schema, previewData) {
    // Update table header
    previewTableHead.innerHTML = ''; // Clear existing header
    const headerRow = previewTableHead.insertRow();
    if (schema.length === 0 && previewData.length === 0) {
      previewTable.classList.add('hidden');
      previewPlaceholder.classList.remove('hidden');
      return;
    } else if(schema.length > 0) {
      schema.forEach(field => {
        const th = document.createElement('th');
        th.textContent = field.name; // Use 'name' from schema DTO
        headerRow.appendChild(th);
      });
    } else if (previewData.length > 0) {
      // Fallback: use keys from first data row if schema somehow empty
      Object.keys(previewData[0]).forEach(key => {
        const th = document.createElement('th');
        th.textContent = key;
        headerRow.appendChild(th);
      });
    }


    // Update table body
    previewTableBody.innerHTML = ''; // Clear existing body
    previewData.forEach(rowData => {
      const row = previewTableBody.insertRow();
      schema.forEach(field => { // Iterate based on schema order
        const cell = row.insertCell();
        const value = rowData[field.name]; // Access data by field name
        cell.textContent = value;
        cell.title = value; // Tooltip for potentially long content
      });
    });

    previewTable.classList.remove('hidden');
    previewPlaceholder.classList.add('hidden');
  }


  function triggerDownload(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // --- Core Logic Functions ---

  async function fetchAndPopulateDataTypes() {
    isLoadingDataTypes = true;
    // Update existing dropdowns to show loading state
    schemaFieldsContainer.querySelectorAll('select[name="dataType"]').forEach(select => {
      select.innerHTML = '<option value="">-- Loading Types... --</option>';
      select.disabled = true;
    });

    try {
      const types = await apiFetch('/datatypes'); // Array of { key, name, category }
      // Group by category
      availableDataTypes = types.reduce((acc, type) => {
        const category = type.category || 'Other';
        if (!acc[category]) {
          acc[category] = [];
        }
        acc[category].push(type);
        return acc;
      }, {});
      isLoadingDataTypes = false;
      // Repopulate all existing dropdowns
      schemaFieldsContainer.querySelectorAll('select[name="dataType"]').forEach(select => {
        const currentValue = select.value; // Preserve current selection if possible
        populateDataTypeSelect(select);
        select.value = currentValue; // Restore selection
      });
    } catch (error) {
      displayError(`Failed to load data types: ${error.message}`);
      isLoadingDataTypes = false;
      // Update dropdowns to show error
      schemaFieldsContainer.querySelectorAll('select[name="dataType"]').forEach(select => {
        select.innerHTML = '<option value="">-- Error Loading Types --</option>';
        select.disabled = true;
      });
    }
  }

  async function updatePreview() {
    const schema = getSchemaFromDOM();
    if (schema.length === 0) {
      updatePreviewTable([], []); // Clear table and show placeholder
      return;
    }

    setPreviewLoading(true);
    clearError(); // Clear previous errors

    try {
      const requestBody = {
        schema: schema,
        rowCount: PREVIEW_ROW_LIMIT // Backend expects rowCount even for preview
        // format isn't strictly needed by backend preview endpoint, but good practice
        // format: formatSelect.value
      };
      const previewData = await apiFetch('/generate/preview', {
        method: 'POST',
        body: requestBody
      });
      updatePreviewTable(schema, previewData); // Update table with fetched data
    } catch (error) {
      displayError(`Preview failed: ${error.message}`);
      updatePreviewTable(schema, []); // Show headers but no data on error
    } finally {
      setPreviewLoading(false);
    }
  }

  async function handleGenerateAndDownload() {
    clearError();
    const schema = getSchemaFromDOM();
    const rowCount = parseInt(rowCountInput.value, 10);
    const format = formatSelect.value;
    const tableName = sqlTableNameInput.value.trim();

    // Frontend Validations
    if (schema.length === 0) {
      displayError("Schema is empty. Please add fields.");
      return;
    }
    if (isNaN(rowCount) || rowCount <= 0) {
      displayError("Please enter a valid number of rows (greater than 0).");
      return;
    }
    if (format.toUpperCase() === 'SQL' && !tableName) {
      displayError("Please enter a table name for SQL format.");
      return;
    }

    setGlobalLoading(true, 'Generating data...');

    try {
      const requestBody = {
        schema: schema,
        rowCount: rowCount,
        format: format,
        tableName: format.toUpperCase() === 'SQL' ? tableName : null
      };

      // Fetch expects a blob for file downloads
      const { blob, filename } = await apiFetch('/generate', {
        method: 'POST',
        body: requestBody,
        expectBlob: true // Signal fetch helper to handle blob response
      });

      triggerDownload(blob, filename);

    } catch (error) {
      displayError(`Generation failed: ${error.message}`);
    } finally {
      setGlobalLoading(false);
    }
  }

  // --- Schema Management Logic ---

  async function populateLoadSchemaDropdown() {
    try {
      // Fetch list of {id, name} from backend
      const schemas = await apiFetch('/schemas');
      loadSchemaSelect.innerHTML = '<option value="">-- Load Saved Schema --</option>'; // Reset
      schemas.forEach(schema => {
        const option = document.createElement('option');
        option.value = schema.id; // Use backend ID as value
        option.textContent = schema.name || `Schema ${schema.id}`;
        loadSchemaSelect.appendChild(option);
      });
    } catch (error) {
      displayError(`Failed to load schema list: ${error.message}`);
      loadSchemaSelect.innerHTML = '<option value="">-- Error Loading --</option>';
    }
  }

  async function handleSaveSchema() {
    const schemaName = schemaNameInput.value.trim();
    const schemaFields = getSchemaFromDOM();
    if (!schemaName) {
      displayError("Please enter a name to save the schema.");
      return;
    }
    if (schemaFields.length === 0) {
      displayError("Cannot save an empty schema.");
      return;
    }

    clearError();
    setGlobalLoading(true, 'Saving schema...');

    try {
      const requestBody = {
        name: schemaName,
        fields: schemaFields // Matches SchemaDefinitionDto on backend
      };
      // Send POST request to save
      const savedSchema = await apiFetch('/schemas', {
        method: 'POST',
        body: requestBody
      });

      alert(`Schema "${savedSchema.name}" saved successfully (ID: ${savedSchema.id})!`);
      await populateLoadSchemaDropdown(); // Refresh dropdown
      // Select the newly saved schema in the dropdown
      loadSchemaSelect.value = savedSchema.id;
      displayShareLink(savedSchema.id, savedSchema.shareLink); // Show share link

    } catch (error) {
      displayError(`Failed to save schema: ${error.message}`);
    } finally {
      setGlobalLoading(false);
    }
  }

  async function handleLoadSchema() {
    const selectedId = loadSchemaSelect.value;
    if (!selectedId) {
      displayError("Please select a schema to load.");
      return;
    }

    clearError();
    setGlobalLoading(true, 'Loading schema...');

    try {
      // Fetch the full schema details by ID
      const loadedSchema = await apiFetch(`/schemas/${selectedId}`); // GET request

      // Clear current fields
      schemaFieldsContainer.innerHTML = '';
      // Add fields from loaded schema
      loadedSchema.fields.forEach(field => {
        addSchemaField(field); // field matches { name, dataType, options }
      });
      schemaNameInput.value = loadedSchema.name; // Set the name input
      alert(`Schema "${loadedSchema.name}" loaded.`);
      debouncedUpdatePreview(); // Update preview after loading
      displayShareLink(loadedSchema.id, loadedSchema.shareLink); // Update share link display

    } catch (error) {
      displayError(`Failed to load schema: ${error.message}`);
    } finally {
      setGlobalLoading(false);
    }
  }

  async function handleDeleteSchema() {
    const selectedId = loadSchemaSelect.value;
    if (!selectedId) {
      displayError("Please select a schema to delete.");
      return;
    }

    // Find the name for confirmation message
    const selectedOption = loadSchemaSelect.options[loadSchemaSelect.selectedIndex];
    const schemaName = selectedOption ? selectedOption.textContent : `Schema ID ${selectedId}`;


    if (!confirm(`Are you sure you want to delete schema "${schemaName}"?`)) {
      return;
    }

    clearError();
    setGlobalLoading(true, 'Deleting schema...');

    try {
      // Send DELETE request to the backend
      await apiFetch(`/schemas/${selectedId}`, {
        method: 'DELETE'
      });

      alert(`Schema "${schemaName}" deleted successfully.`);
      // Optionally clear fields if the deleted schema was loaded
      // const currentLoadedName = schemaNameInput.value;
      // if (currentLoadedName === schemaName) { // Rough check
      //     schemaFieldsContainer.innerHTML = '';
      //     schemaNameInput.value = '';
      //     debouncedUpdatePreview();
      // }
      await populateLoadSchemaDropdown(); // Refresh dropdown

    } catch (error) {
      displayError(`Failed to delete schema: ${error.message}`);
    } finally {
      setGlobalLoading(false);
    }
  }

  // Generate and display the shareable frontend link
  function displayShareLink(schemaId, apiLink) {
    if (!schemaId) {
      shareLinkDisplay.classList.add('hidden');
      return;
    }
    // Construct frontend URL with hash for loading
    const shareUrl = `${window.location.origin}${window.location.pathname}#load=${schemaId}`;
    shareLinkAnchor.href = shareUrl;
    shareLinkAnchor.textContent = shareUrl; // Show the full frontend link
    apiLinkSpan.textContent = apiLink || 'N/A'; // Show the API link provided by backend
    shareLinkDisplay.classList.remove('hidden');
  }

  // Load schema from URL hash (#load=ID) on page load
  async function loadSchemaFromUrl() {
    const hash = window.location.hash;
    if (hash && hash.startsWith('#load=')) {
      const schemaId = hash.substring(6); // Get ID after #load=
      if (schemaId && /^\d+$/.test(schemaId)) { // Check if it's a number
        clearError();
        setGlobalLoading(true, `Loading schema ${schemaId} from URL...`);
        try {
          const loadedSchema = await apiFetch(`/schemas/${schemaId}`);
          schemaFieldsContainer.innerHTML = ''; // Clear default/existing
          loadedSchema.fields.forEach(field => addSchemaField(field));
          schemaNameInput.value = loadedSchema.name;
          alert(`Schema "${loadedSchema.name}" (ID: ${schemaId}) loaded from URL!`);
          debouncedUpdatePreview();
          displayShareLink(loadedSchema.id, loadedSchema.shareLink);
          // Select it in the dropdown if possible (needs dropdown to be populated first)
          await populateLoadSchemaDropdown(); // Ensure dropdown is populated
          loadSchemaSelect.value = schemaId;

        } catch (error) {
          displayError(`Failed to load schema ${schemaId} from URL: ${error.message}`);
          // Optionally clear the hash if loading fails
          // history.replaceState(null, null, window.location.pathname + window.location.search);
        } finally {
          setGlobalLoading(false);
        }

      }
    }
  }


  // --- Event Listeners ---
  addFieldBtn.addEventListener('click', () => addSchemaField());

  formatSelect.addEventListener('change', () => {
    sqlOptionsDiv.classList.toggle('hidden', formatSelect.value.toUpperCase() !== 'SQL');
  });

  generateBtn.addEventListener('click', handleGenerateAndDownload);

  // Dark Mode
  darkModeToggle.addEventListener('click', () => {
    document.body.classList.toggle('dark');
    localStorage.setItem('darkMode', document.body.classList.contains('dark'));
  });

  // Apply saved dark mode preference
  if (localStorage.getItem('darkMode') === 'true') {
    document.body.classList.add('dark');
  }

  // Schema Management Listeners
  saveSchemaBtn.addEventListener('click', handleSaveSchema);
  loadSchemaBtn.addEventListener('click', handleLoadSchema);
  deleteSchemaBtn.addEventListener('click', handleDeleteSchema);

  // --- Initialisation ---
  async function initializeApp() {
    addSchemaField({ name: 'id', dataType: 'string.uuid' }); // Default fields
    addSchemaField({ name: 'name', dataType: 'person.fullName' });
    addSchemaField({ name: 'email', dataType: 'internet.email' });
    await fetchAndPopulateDataTypes(); // Fetch types needed for dropdowns
    await populateLoadSchemaDropdown(); // Fetch saved schema list
    await loadSchemaFromUrl(); // Check URL after dropdowns are ready
    debouncedUpdatePreview(); // Initial preview based on default/URL loaded schema
  }

  initializeApp(); // Start the application

});