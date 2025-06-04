document.addEventListener('DOMContentLoaded', () => {
  // --- Configuration ---
  const API_BASE_URL = 'http://localhost:8080/api';
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
  const loadingIndicator = document.getElementById('loadingIndicator');
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

  // Schema Formatting Elements
  const toggleFormattingRules = document.getElementById('toggleFormattingRules');
  const schemaFormattingPanel = document.getElementById('schemaFormattingPanel');
  const addCategoryRuleBtn = document.getElementById('addCategoryRuleBtn');
  const addConditionalRuleBtn = document.getElementById('addConditionalRuleBtn');
  const previewSchemaFormatting = document.getElementById('previewSchemaFormatting');
  const resetSchemaFormatting = document.getElementById('resetSchemaFormatting');
  const categoryRulesContainer = document.getElementById('categoryRulesContainer');
  const conditionalRulesContainer = document.getElementById('conditionalRulesContainer');

  // Advanced Options Modal Elements
  const advancedOptionsModal = document.getElementById('advancedOptionsModal');
  const modalClose = document.querySelector('.modal-close');
  const applyAdvancedOptions = document.getElementById('applyAdvancedOptions');
  const cancelAdvancedOptions = document.getElementById('cancelAdvancedOptions');

  // Templates
  const categoryRuleTemplate = document.getElementById('categoryRuleTemplate');
  const conditionalRuleTemplate = document.getElementById('conditionalRuleTemplate');

  // --- State Variables ---
  let availableDataTypes = {};
  let isLoadingDataTypes = false;
  let currentFieldBeingEdited = null; // For advanced options modal
  let currentFieldOptions = {}; // Store enhanced options for each field

  // --- API Helper Functions ---
  async function apiFetch(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const defaultOptions = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    };
    const mergedOptions = { ...defaultOptions, ...options };

    if (mergedOptions.body && typeof mergedOptions.body === 'object') {
      mergedOptions.body = JSON.stringify(mergedOptions.body);
    }

    try {
      const response = await fetch(url, mergedOptions);
      const contentType = response.headers.get('content-type');

      if (options.expectBlob || (contentType && !contentType.includes('application/json') && response.ok && options.method?.toUpperCase() !== 'DELETE')) {
        if (!response.ok) {
          let errorText = `HTTP error ${response.status}`;
          try { errorText = await response.text(); } catch(e){}
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
        return { blob, filename };
      }

      if (response.status === 204) {
        return null;
      }

      const data = await response.json();
      if (!response.ok) {
        const errorMessage = data?.message || data?.error || `HTTP error ${response.status}`;
        throw new Error(errorMessage);
      }
      return data;

    } catch (error) {
      console.error(`API Fetch Error (${options.method || 'GET'} ${endpoint}):`, error);
      throw error;
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
      generateBtn.disabled = true;
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
    }
  }

  // --- Enhanced Field Management ---
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

    const sortedCategories = Object.keys(availableDataTypes).sort();

    for (const category of sortedCategories) {
      const optgroup = document.createElement('optgroup');
      optgroup.label = category;
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

  function generateFieldId() {
    return 'field_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  function addSchemaField(fieldData = { name: '', dataType: '', options: '' }) {
    const templateContent = schemaFieldTemplate.content.cloneNode(true);
    const fieldRow = templateContent.querySelector('.schema-field-row');
    const fieldId = generateFieldId();
    fieldRow.dataset.fieldId = fieldId;

    const nameInput = fieldRow.querySelector('input[name="fieldName"]');
    const typeSelect = fieldRow.querySelector('select[name="dataType"]');
    const optionsInput = fieldRow.querySelector('input[name="fieldOptions"]');
    const advancedBtn = fieldRow.querySelector('.advanced-options-btn');
    const removeBtn = fieldRow.querySelector('.remove-field-btn');

    populateDataTypeSelect(typeSelect);

    // Set initial values
    nameInput.value = fieldData.name || '';
    typeSelect.value = fieldData.dataType || '';
    optionsInput.value = fieldData.options || '';

    // Initialize enhanced options for this field
    currentFieldOptions[fieldId] = {
      baseType: fieldData.dataType || null,
      baseOptions: fieldData.options || null,
      formatting: null,
      dependency: null
    };

    // Event listeners
    removeBtn.addEventListener('click', () => {
      delete currentFieldOptions[fieldId];
      fieldRow.remove();
      debouncedUpdatePreview();
    });

    advancedBtn.addEventListener('click', () => {
      openAdvancedOptionsModal(fieldId);
    });

    nameInput.addEventListener('input', debouncedUpdatePreview);
    typeSelect.addEventListener('change', () => {
      // Update the base type when data type changes
      if (currentFieldOptions[fieldId]) {
        currentFieldOptions[fieldId].baseType = typeSelect.value;
      }
      debouncedUpdatePreview();
    });
    optionsInput.addEventListener('input', () => {
      // Update base options when basic options change
      if (currentFieldOptions[fieldId]) {
        currentFieldOptions[fieldId].baseOptions = optionsInput.value;
      }
      debouncedUpdatePreview();
    });

    schemaFieldsContainer.appendChild(fieldRow);
  }

  // --- Advanced Options Modal ---
  function openAdvancedOptionsModal(fieldId) {
    currentFieldBeingEdited = fieldId;
    const fieldOptions = currentFieldOptions[fieldId] || {};
    const fieldRow = document.querySelector(`[data-field-id="${fieldId}"]`);
    const fieldName = fieldRow.querySelector('input[name="fieldName"]').value;

    // Populate modal with current values
    document.getElementById('modalBasicOptions').value = fieldOptions.baseOptions || '';

    // Formatting tab
    const formatting = fieldOptions.formatting || {};
    document.getElementById('fieldCase').value = formatting.caseTransform || '';
    document.getElementById('fieldPrefix').value = formatting.prefix || '';
    document.getElementById('fieldSuffix').value = formatting.suffix || '';
    document.getElementById('fixedLength').value = formatting.fixedLength?.length || '';
    document.getElementById('paddingPosition').value = formatting.fixedLength?.padding?.position || 'RIGHT';
    document.getElementById('paddingChar').value = formatting.fixedLength?.padding?.character || ' ';
    document.getElementById('truncateFrom').value = formatting.fixedLength?.truncateFrom || 'END';

    // Dependencies tab
    const dependency = fieldOptions.dependency || {};
    document.getElementById('dependencyExpression').value = dependency.expression || '';
    document.getElementById('dependsOn').value = dependency.dependsOn?.join(',') || '';

    // Update modal title
    document.querySelector('.modal-header h3').textContent = `Advanced Options - ${fieldName || 'Field'}`;

    advancedOptionsModal.classList.remove('hidden');
  }

  function closeAdvancedOptionsModal() {
    advancedOptionsModal.classList.add('hidden');
    currentFieldBeingEdited = null;
  }

  function applyAdvancedOptionsFromModal() {
    if (!currentFieldBeingEdited) return;

    const fieldOptions = currentFieldOptions[currentFieldBeingEdited] || {};

    // Update base options
    fieldOptions.baseOptions = document.getElementById('modalBasicOptions').value || null;

    // Update formatting
    const formatting = {};
    const caseTransform = document.getElementById('fieldCase').value;
    const prefix = document.getElementById('fieldPrefix').value;
    const suffix = document.getElementById('fieldSuffix').value;
    const fixedLength = document.getElementById('fixedLength').value;
    const paddingPosition = document.getElementById('paddingPosition').value;
    const paddingChar = document.getElementById('paddingChar').value;
    const truncateFrom = document.getElementById('truncateFrom').value;

    if (caseTransform) formatting.caseTransform = caseTransform;
    if (prefix) formatting.prefix = prefix;
    if (suffix) formatting.suffix = suffix;
    if (fixedLength) {
      formatting.fixedLength = {
        length: parseInt(fixedLength),
        truncateFrom: truncateFrom,
        padding: {
          position: paddingPosition,
          character: paddingChar || ' '
        }
      };
    }

    fieldOptions.formatting = Object.keys(formatting).length > 0 ? formatting : null;

    // Update dependencies
    const dependency = {};
    const expression = document.getElementById('dependencyExpression').value;
    const dependsOn = document.getElementById('dependsOn').value;

    if (expression) dependency.expression = expression;
    if (dependsOn) {
      dependency.dependsOn = dependsOn.split(',').map(s => s.trim()).filter(s => s);
    }

    fieldOptions.dependency = Object.keys(dependency).length > 0 ? dependency : null;

    // Update the stored options
    currentFieldOptions[currentFieldBeingEdited] = fieldOptions;

    // Update the basic options field to show it has advanced options
    const fieldRow = document.querySelector(`[data-field-id="${currentFieldBeingEdited}"]`);
    const basicOptionsInput = fieldRow.querySelector('input[name="fieldOptions"]');
    basicOptionsInput.value = fieldOptions.baseOptions || '';

    // Visual indicator that advanced options are set
    const advancedBtn = fieldRow.querySelector('.advanced-options-btn');
    if (fieldOptions.formatting || fieldOptions.dependency) {
      advancedBtn.style.backgroundColor = 'var(--jpmc-success-bg)';
      advancedBtn.style.color = 'var(--jpmc-success-text)';
      advancedBtn.title = 'Advanced options configured';
    } else {
      advancedBtn.style.backgroundColor = '';
      advancedBtn.style.color = '';
      advancedBtn.title = 'Advanced Field Formatting';
    }

    closeAdvancedOptionsModal();
    debouncedUpdatePreview();
  }

  // --- Schema Formatting Rules ---
  function toggleSchemaFormattingPanel() {
    schemaFormattingPanel.classList.toggle('hidden');
    const isVisible = !schemaFormattingPanel.classList.contains('hidden');
    toggleFormattingRules.textContent = isVisible ? '⚙️ Hide Schema Formatting' : '⚙️ Configure Schema Formatting';
  }

  function addCategoryRule() {
    const templateContent = categoryRuleTemplate.content.cloneNode(true);
    const ruleDiv = templateContent.querySelector('.category-rule');

    const removeBtn = ruleDiv.querySelector('.remove-rule-btn');
    removeBtn.addEventListener('click', () => {
      ruleDiv.remove();
      debouncedUpdatePreview();
    });

    // Add change listeners for preview updates
    ruleDiv.querySelectorAll('select, input').forEach(element => {
      element.addEventListener('change', debouncedUpdatePreview);
      element.addEventListener('input', debouncedUpdatePreview);
    });

    categoryRulesContainer.appendChild(ruleDiv);
  }

  function addConditionalRule() {
    const templateContent = conditionalRuleTemplate.content.cloneNode(true);
    const ruleDiv = templateContent.querySelector('.conditional-rule');

    const removeBtn = ruleDiv.querySelector('.remove-rule-btn');
    removeBtn.addEventListener('click', () => {
      ruleDiv.remove();
      debouncedUpdatePreview();
    });

    // Add change listeners for preview updates
    ruleDiv.querySelectorAll('select, input').forEach(element => {
      element.addEventListener('change', debouncedUpdatePreview);
      element.addEventListener('input', debouncedUpdatePreview);
    });

    conditionalRulesContainer.appendChild(ruleDiv);
  }

  function getSchemaFormattingRules() {
    const rules = {
      globalRules: {},
      categoryRules: {},
      conditionalRules: []
    };

    // Global rules
    const globalCase = document.getElementById('globalCase').value;
    const globalPrefix = document.getElementById('globalPrefix').value;
    const globalSuffix = document.getElementById('globalSuffix').value;
    const globalMaxLength = document.getElementById('globalMaxLength').value;
    const globalDateFormat = document.getElementById('globalDateFormat').value;
    const globalNumberFormat = document.getElementById('globalNumberFormat').value;

    if (globalCase) rules.globalRules.defaultCase = globalCase;
    if (globalPrefix) rules.globalRules.defaultPrefix = globalPrefix;
    if (globalSuffix) rules.globalRules.defaultSuffix = globalSuffix;
    if (globalMaxLength) rules.globalRules.defaultMaxLength = parseInt(globalMaxLength);
    if (globalDateFormat) rules.globalRules.dateFormat = globalDateFormat;
    if (globalNumberFormat) rules.globalRules.numberFormat = globalNumberFormat;

    // Category rules
    categoryRulesContainer.querySelectorAll('.category-rule').forEach(ruleDiv => {
      const category = ruleDiv.querySelector('.category-select').value;
      const prefix = ruleDiv.querySelector('.category-prefix').value;
      const suffix = ruleDiv.querySelector('.category-suffix').value;
      const caseTransform = ruleDiv.querySelector('.category-case').value;
      const override = ruleDiv.querySelector('.category-override').checked;

      if (category) {
        const formatting = {};
        if (prefix) formatting.prefix = prefix;
        if (suffix) formatting.suffix = suffix;
        if (caseTransform) formatting.caseTransform = caseTransform;

        rules.categoryRules[category] = {
          category: category,
          formatting: formatting,
          overrideFieldRules: override
        };
      }
    });

    // Conditional rules
    conditionalRulesContainer.querySelectorAll('.conditional-rule').forEach(ruleDiv => {
      const condition = ruleDiv.querySelector('.condition-expression').value;
      const priority = ruleDiv.querySelector('.condition-priority').value;
      const prefix = ruleDiv.querySelector('.conditional-prefix').value;
      const suffix = ruleDiv.querySelector('.conditional-suffix').value;
      const caseTransform = ruleDiv.querySelector('.conditional-case').value;

      if (condition) {
        const formatting = {};
        if (prefix) formatting.prefix = prefix;
        if (suffix) formatting.suffix = suffix;
        if (caseTransform) formatting.caseTransform = caseTransform;

        rules.conditionalRules.push({
          condition: condition,
          priority: priority ? parseInt(priority) : 0,
          formatting: formatting
        });
      }
    });

    return rules;
  }

  function resetSchemaFormattingRules() {
    // Clear global rules
    document.getElementById('globalCase').value = '';
    document.getElementById('globalPrefix').value = '';
    document.getElementById('globalSuffix').value = '';
    document.getElementById('globalMaxLength').value = '';
    document.getElementById('globalDateFormat').value = '';
    document.getElementById('globalNumberFormat').value = '';

    // Clear category rules
    categoryRulesContainer.innerHTML = '';

    // Clear conditional rules
    conditionalRulesContainer.innerHTML = '';

    debouncedUpdatePreview();
  }

  // --- Data Collection Functions ---
  function getSchemaFromDOM() {
    const fields = [];
    schemaFieldsContainer.querySelectorAll('.schema-field-row').forEach(row => {
      const fieldId = row.dataset.fieldId;
      const nameInput = row.querySelector('input[name="fieldName"]');
      const typeSelect = row.querySelector('select[name="dataType"]');

      if (nameInput.value.trim() && typeSelect.value) {
        const fieldOptions = currentFieldOptions[fieldId] || {};

        // Create enhanced field options JSON
        const enhancedOptions = {
          baseType: fieldOptions.baseType || typeSelect.value,
          baseOptions: fieldOptions.baseOptions || null,
          formatting: fieldOptions.formatting || null,
          dependency: fieldOptions.dependency || null
        };

        fields.push({
          name: nameInput.value.trim(),
          dataType: typeSelect.value,
          options: JSON.stringify(enhancedOptions) // Send as JSON string
        });
      }
    });
    return fields;
  }

  // --- Tab System ---
  function initializeTabSystem() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetTab = btn.dataset.tab;

        // Remove active class from all tabs and contents
        tabBtns.forEach(b => b.classList.remove('active'));
        tabContents.forEach(c => c.classList.remove('active'));

        // Add active class to clicked tab and corresponding content
        btn.classList.add('active');
        document.getElementById(targetTab + '-tab').classList.add('active');
      });
    });
  }

  // --- Preview and Generation ---
  let debounceTimer;
  function debounce(func, delay) {
    return function() {
      const context = this;
      const args = arguments;
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => func.apply(context, args), delay);
    };
  }
  const debouncedUpdatePreview = debounce(updatePreview, 400);

  function updatePreviewTable(schema, previewData) {
    previewTableHead.innerHTML = '';
    const headerRow = previewTableHead.insertRow();

    if (schema.length === 0 && previewData.length === 0) {
      previewTable.classList.add('hidden');
      previewPlaceholder.classList.remove('hidden');
      return;
    } else if(schema.length > 0) {
      schema.forEach(field => {
        const th = document.createElement('th');
        th.textContent = field.name;
        headerRow.appendChild(th);
      });
    } else if (previewData.length > 0) {
      Object.keys(previewData[0]).forEach(key => {
        const th = document.createElement('th');
        th.textContent = key;
        headerRow.appendChild(th);
      });
    }

    previewTableBody.innerHTML = '';
    previewData.forEach(rowData => {
      const row = previewTableBody.insertRow();
      schema.forEach(field => {
        const cell = row.insertCell();
        const value = rowData[field.name];
        cell.textContent = value;
        cell.title = value;
      });
    });

    previewTable.classList.remove('hidden');
    previewPlaceholder.classList.add('hidden');
  }

  async function updatePreview() {
    const schema = getSchemaFromDOM();
    if (schema.length === 0) {
      updatePreviewTable([], []);
      return;
    }

    setPreviewLoading(true);
    clearError();

    try {
      const schemaFormattingRules = getSchemaFormattingRules();
      const requestBody = {
        schema: schema,
        rowCount: PREVIEW_ROW_LIMIT,
        schemaFormattingRules: JSON.stringify(schemaFormattingRules)
      };

      const previewData = await apiFetch('/generate/preview', {
        method: 'POST',
        body: requestBody
      });

      updatePreviewTable(schema, previewData);
    } catch (error) {
      displayError(`Preview failed: ${error.message}`);
      updatePreviewTable(schema, []);
    } finally {
      setPreviewLoading(false);
    }
  }

  // --- Core Logic Functions ---
  async function fetchAndPopulateDataTypes() {
    isLoadingDataTypes = true;
    schemaFieldsContainer.querySelectorAll('select[name="dataType"]').forEach(select => {
      select.innerHTML = '<option value="">-- Loading Types... --</option>';
      select.disabled = true;
    });

    try {
      const types = await apiFetch('/datatypes');
      availableDataTypes = types.reduce((acc, type) => {
        const category = type.category || 'Other';
        if (!acc[category]) {
          acc[category] = [];
        }
        acc[category].push(type);
        return acc;
      }, {});
      isLoadingDataTypes = false;

      schemaFieldsContainer.querySelectorAll('select[name="dataType"]').forEach(select => {
        const currentValue = select.value;
        populateDataTypeSelect(select);
        select.value = currentValue;
      });
    } catch (error) {
      displayError(`Failed to load data types: ${error.message}`);
      isLoadingDataTypes = false;
      schemaFieldsContainer.querySelectorAll('select[name="dataType"]').forEach(select => {
        select.innerHTML = '<option value="">-- Error Loading Types --</option>';
        select.disabled = true;
      });
    }
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

  async function handleGenerateAndDownload() {
    clearError();
    const schema = getSchemaFromDOM();
    const rowCount = parseInt(rowCountInput.value, 10);
    const format = formatSelect.value;
    const tableName = sqlTableNameInput.value.trim();
    const schemaFormattingRules = getSchemaFormattingRules();

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
        tableName: format.toUpperCase() === 'SQL' ? tableName : null,
        schemaFormattingRules: JSON.stringify(schemaFormattingRules)
      };

      const { blob, filename } = await apiFetch('/generate', {
        method: 'POST',
        body: requestBody,
        expectBlob: true
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
      const schemas = await apiFetch('/schemas');
      loadSchemaSelect.innerHTML = '<option value="">-- Load Saved Schema --</option>';
      schemas.forEach(schema => {
        const option = document.createElement('option');
        option.value = schema.id;
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
    const formattingRules = getSchemaFormattingRules();

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
        fields: schemaFields,
        formattingRules: formattingRules
      };

      const savedSchema = await apiFetch('/schemas', {
        method: 'POST',
        body: requestBody
      });

      alert(`Schema "${savedSchema.name}" saved successfully (ID: ${savedSchema.id})!`);
      await populateLoadSchemaDropdown();
      loadSchemaSelect.value = savedSchema.id;
      displayShareLink(savedSchema.id, savedSchema.shareLink);

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
      const loadedSchema = await apiFetch(`/schemas/${selectedId}`);

      // Clear current fields and options
      schemaFieldsContainer.innerHTML = '';
      currentFieldOptions = {};

      // Load fields with their enhanced options
      loadedSchema.fields.forEach(field => {
        let enhancedOptions = {};
        try {
          if (field.options) {
            enhancedOptions = JSON.parse(field.options);
          }
        } catch (e) {
          // Fallback for old format
          enhancedOptions = {
            baseType: field.dataType,
            baseOptions: field.options,
            formatting: null,
            dependency: null
          };
        }

        addSchemaField({
          name: field.name,
          dataType: field.dataType,
          options: enhancedOptions.baseOptions || ''
        });

        // Set the enhanced options for the field
        const fieldId = Object.keys(currentFieldOptions)[Object.keys(currentFieldOptions).length - 1];
        currentFieldOptions[fieldId] = enhancedOptions;
      });

      // Load schema formatting rules
      if (loadedSchema.formattingRules) {
        loadSchemaFormattingRules(loadedSchema.formattingRules);
      }

      schemaNameInput.value = loadedSchema.name;
      alert(`Schema "${loadedSchema.name}" loaded.`);
      debouncedUpdatePreview();
      displayShareLink(loadedSchema.id, loadedSchema.shareLink);

    } catch (error) {
      displayError(`Failed to load schema: ${error.message}`);
    } finally {
      setGlobalLoading(false);
    }
  }

  function loadSchemaFormattingRules(rules) {
    // Reset current rules
    resetSchemaFormattingRules();

    // Load global rules
    if (rules.globalRules) {
      const global = rules.globalRules;
      if (global.defaultCase) document.getElementById('globalCase').value = global.defaultCase;
      if (global.defaultPrefix) document.getElementById('globalPrefix').value = global.defaultPrefix;
      if (global.defaultSuffix) document.getElementById('globalSuffix').value = global.defaultSuffix;
      if (global.defaultMaxLength) document.getElementById('globalMaxLength').value = global.defaultMaxLength;
      if (global.dateFormat) document.getElementById('globalDateFormat').value = global.dateFormat;
      if (global.numberFormat) document.getElementById('globalNumberFormat').value = global.numberFormat;
    }

    // Load category rules
    if (rules.categoryRules) {
      Object.values(rules.categoryRules).forEach(rule => {
        addCategoryRule();
        const ruleDiv = categoryRulesContainer.lastElementChild;
        ruleDiv.querySelector('.category-select').value = rule.category;
        if (rule.formatting.prefix) ruleDiv.querySelector('.category-prefix').value = rule.formatting.prefix;
        if (rule.formatting.suffix) ruleDiv.querySelector('.category-suffix').value = rule.formatting.suffix;
        if (rule.formatting.caseTransform) ruleDiv.querySelector('.category-case').value = rule.formatting.caseTransform;
        ruleDiv.querySelector('.category-override').checked = rule.overrideFieldRules || false;
      });
    }

    // Load conditional rules
    if (rules.conditionalRules) {
      rules.conditionalRules.forEach(rule => {
        addConditionalRule();
        const ruleDiv = conditionalRulesContainer.lastElementChild;
        ruleDiv.querySelector('.condition-expression').value = rule.condition;
        ruleDiv.querySelector('.condition-priority').value = rule.priority || 0;
        if (rule.formatting.prefix) ruleDiv.querySelector('.conditional-prefix').value = rule.formatting.prefix;
        if (rule.formatting.suffix) ruleDiv.querySelector('.conditional-suffix').value = rule.formatting.suffix;
        if (rule.formatting.caseTransform) ruleDiv.querySelector('.conditional-case').value = rule.formatting.caseTransform;
      });
    }
  }

  async function handleDeleteSchema() {
    const selectedId = loadSchemaSelect.value;
    if (!selectedId) {
      displayError("Please select a schema to delete.");
      return;
    }

    const selectedOption = loadSchemaSelect.options[loadSchemaSelect.selectedIndex];
    const schemaName = selectedOption ? selectedOption.textContent : `Schema ID ${selectedId}`;

    if (!confirm(`Are you sure you want to delete schema "${schemaName}"?`)) {
      return;
    }

    clearError();
    setGlobalLoading(true, 'Deleting schema...');

    try {
      await apiFetch(`/schemas/${selectedId}`, {
        method: 'DELETE'
      });

      alert(`Schema "${schemaName}" deleted successfully.`);
      await populateLoadSchemaDropdown();

    } catch (error) {
      displayError(`Failed to delete schema: ${error.message}`);
    } finally {
      setGlobalLoading(false);
    }
  }

  function displayShareLink(schemaId, apiLink) {
    if (!schemaId) {
      shareLinkDisplay.classList.add('hidden');
      return;
    }
    const shareUrl = `${window.location.origin}${window.location.pathname}#load=${schemaId}`;
    shareLinkAnchor.href = shareUrl;
    shareLinkAnchor.textContent = shareUrl;
    apiLinkSpan.textContent = apiLink || 'N/A';
    shareLinkDisplay.classList.remove('hidden');
  }

  async function loadSchemaFromUrl() {
    const hash = window.location.hash;
    if (hash && hash.startsWith('#load=')) {
      const schemaId = hash.substring(6);
      if (schemaId && /^\d+$/.test(schemaId)) {
        clearError();
        setGlobalLoading(true, `Loading schema ${schemaId} from URL...`);
        try {
          const loadedSchema = await apiFetch(`/schemas/${schemaId}`);

          // Clear and load schema
          schemaFieldsContainer.innerHTML = '';
          currentFieldOptions = {};

          loadedSchema.fields.forEach(field => {
            let enhancedOptions = {};
            try {
              if (field.options) {
                enhancedOptions = JSON.parse(field.options);
              }
            } catch (e) {
              enhancedOptions = {
                baseType: field.dataType,
                baseOptions: field.options,
                formatting: null,
                dependency: null
              };
            }

            addSchemaField({
              name: field.name,
              dataType: field.dataType,
              options: enhancedOptions.baseOptions || ''
            });

            const fieldId = Object.keys(currentFieldOptions)[Object.keys(currentFieldOptions).length - 1];
            currentFieldOptions[fieldId] = enhancedOptions;
          });

          if (loadedSchema.formattingRules) {
            loadSchemaFormattingRules(loadedSchema.formattingRules);
          }

          schemaNameInput.value = loadedSchema.name;
          alert(`Schema "${loadedSchema.name}" (ID: ${schemaId}) loaded from URL!`);
          debouncedUpdatePreview();
          displayShareLink(loadedSchema.id, loadedSchema.shareLink);

          await populateLoadSchemaDropdown();
          loadSchemaSelect.value = schemaId;

        } catch (error) {
          displayError(`Failed to load schema ${schemaId} from URL: ${error.message}`);
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

  // Schema Management Listeners
  saveSchemaBtn.addEventListener('click', handleSaveSchema);
  loadSchemaBtn.addEventListener('click', handleLoadSchema);
  deleteSchemaBtn.addEventListener('click', handleDeleteSchema);

  // Schema Formatting Listeners
  toggleFormattingRules.addEventListener('click', toggleSchemaFormattingPanel);
  addCategoryRuleBtn.addEventListener('click', addCategoryRule);
  addConditionalRuleBtn.addEventListener('click', addConditionalRule);
  previewSchemaFormatting.addEventListener('click', debouncedUpdatePreview);
  resetSchemaFormatting.addEventListener('click', resetSchemaFormattingRules);

  // Global formatting rule listeners
  ['globalCase', 'globalPrefix', 'globalSuffix', 'globalMaxLength', 'globalDateFormat', 'globalNumberFormat'].forEach(id => {
    const element = document.getElementById(id);
    if (element) {
      element.addEventListener('change', debouncedUpdatePreview);
      element.addEventListener('input', debouncedUpdatePreview);
    }
  });

  // Advanced Options Modal Listeners
  modalClose.addEventListener('click', closeAdvancedOptionsModal);
  cancelAdvancedOptions.addEventListener('click', closeAdvancedOptionsModal);
  applyAdvancedOptions.addEventListener('click', applyAdvancedOptionsFromModal);

  // Close modal when clicking outside
  advancedOptionsModal.addEventListener('click', (e) => {
    if (e.target === advancedOptionsModal) {
      closeAdvancedOptionsModal();
    }
  });

  // Apply saved dark mode preference
  if (localStorage.getItem('darkMode') === 'true') {
    document.body.classList.add('dark');
  }

  // --- Initialization ---
  async function initializeApp() {
    // Initialize tab system
    initializeTabSystem();

    // Add default fields
    addSchemaField({ name: 'id', dataType: 'String.random', options: '10' });
    addSchemaField({ name: 'firstName', dataType: 'Name.firstName', options: '' });
    addSchemaField({ name: 'lastName', dataType: 'Name.lastName', options: '' });
    addSchemaField({ name: 'email', dataType: 'Internet.emailAddress', options: '' });

    // Initialize data and load from URL if present
    await fetchAndPopulateDataTypes();
    await populateLoadSchemaDropdown();
    await loadSchemaFromUrl();
    debouncedUpdatePreview();
  }

  initializeApp();
});
