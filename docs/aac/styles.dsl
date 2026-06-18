styles {
    # --- C4 base element types ---
    element "Person" {
        shape Person
        background #08427b
        color #ffffff
    }
    element "Software System" {
        background #1168bd
        color #ffffff
    }
    element "Container" {
        background #438dd5
        color #ffffff
    }
    element "Component" {
        background #85bbf0
        color #000000
        shape RoundedBox
    }
    element "Database" {
        shape Cylinder
        background #228b22
        color #ffffff
    }
    element "MessageBus" {
        shape Pipe
        background #e07a5f
        color #ffffff
    }
    element "External" {
        background #999999
        color #ffffff
    }

    # --- Security concern tags (xem views/security.dsl) ---
    element "Ingress" {
        stroke #ff8c00
        strokeWidth 5
    }
    element "InternalApi" {
        stroke #2e86de
        strokeWidth 5
    }
    element "Sensitive" {
        stroke #c0392b
        strokeWidth 6
    }
    element "Standin" {
        opacity 55
        border dashed
    }

    # --- Deployment / infrastructure ---
    element "Infrastructure Node" {
        background #ffffff
        color #000000
    }
}
