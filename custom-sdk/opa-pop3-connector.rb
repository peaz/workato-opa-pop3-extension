{
  title: 'POP3 Connector',
  secure_tunnel: true,

  connection: {
    fields: [
      { name: 'profile', hint: 'POP3 profile name configured on OPA' }
    ],
    authorization: { type: 'none' }
  },

  test: lambda do |connection|
    get("http://localhost/ext/#{connection['profile']}/test")
      .headers('X-Workato-Connector': 'enforce')
  end,

  actions: {
    getEmail: {
      title: 'Retrieve Email',
      description: 'Retrieve Email via POP3',
      input_fields: lambda do |object_definitions|
        object_definitions['input']
      end,
      output_fields: lambda do |object_definitions|
        object_definitions['email']
      end,

      execute: lambda do |connection, input|
        response = post("http://localhost/ext/#{connection['profile']}/getEmail", input)
                    .headers('X-Workato-Connector': 'enforce')
        status = response.dig('status')
        message = response.dig('message')
        if status == "error"
          error(message || "Unknown error from POP3 extension")
        else
          response
        end
      end
    }
  },

  object_definitions: {
    email: {
      fields: lambda do
        [
          {
            "control_type": "text",
            "label": "Message ID",
            "type": "string",
            "name": "message_id",
            "optional": true
          },
          {
            "control_type": "text",
            "label": "From",
            "type": "string",
            "name": "from",
            "optional": false
          },
          {
            "control_type": "text",
            "label": "To",
            "type": "string",
            "name": "to",
            "optional": false
          },
          {
            "control_type": "text",
            "label": "Subject",
            "type": "string",
            "name": "subject",
            "optional": false
          },
          {
            "control_type": "timestamp",
            "label": "Date",
            "type": "datetime",
            "name": "date",
            "optional": false
          },
          {
            "control_type": "text",
            "label": "Content Type",
            "type": "string",
            "name": "content_type",
            "optional": true
          },
          {
            "control_type": "text-area",
            "label": "Body",
            "type": "string",
            "name": "body",
            "optional": false
          },
          {
            "control_type": "array",
            "label": "Attachments",
            "type": "array",
            "name": "attachments",
            "optional": true,
            "fields": [
              {
                "control_type": "text",
                "label": "Filename",
                "type": "string",
                "name": "filename",
                "optional": false
              },
              {
                "control_type": "text",
                "label": "Content",
                "type": "string",
                "name": "content",
                "optional": false
              },
              {
                "control_type": "text",
                "label": "Content Type",
                "type": "string",
                "name": "content_type",
                "optional": false
              }
            ]
          }
        ]
      end
    },

    input: {
      fields: lambda do
        [
          {
            "control_type": "text",
            "label": "Message ID",
            "type": "string",
            "name": "message_id",
            "optional": true,
            "hint": "Retrieve a specific message by ID. If empty, retrieves the oldest unread message"
          },
          {
            "control_type": "checkbox",
            "label": "Delete After Retrieve",
            "type": "boolean",
            "name": "delete_after_retrieve",
            "optional": false,
            "default": false,
            "hint": "Delete the message from server after retrieving"
          }
        ]
      end
    }
  }
}