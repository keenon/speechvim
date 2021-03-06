// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: deepspeech.proto

package io.github.keenon.voicecode;

/**
 * Protobuf type {@code Request}
 */
public  final class Request extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:Request)
    RequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use Request.newBuilder() to construct.
  private Request(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Request() {
    enable_ = false;
    insertMode_ = false;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private Request(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownFieldProto3(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 8: {

            enable_ = input.readBool();
            break;
          }
          case 16: {

            insertMode_ = input.readBool();
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return io.github.keenon.voicecode.DeepSpeechProto.internal_static_Request_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return io.github.keenon.voicecode.DeepSpeechProto.internal_static_Request_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.github.keenon.voicecode.Request.class, io.github.keenon.voicecode.Request.Builder.class);
  }

  public static final int ENABLE_FIELD_NUMBER = 1;
  private boolean enable_;
  /**
   * <pre>
   * If false, stop speech transcript for the moment
   * </pre>
   *
   * <code>bool enable = 1;</code>
   */
  public boolean getEnable() {
    return enable_;
  }

  public static final int INSERTMODE_FIELD_NUMBER = 2;
  private boolean insertMode_;
  /**
   * <pre>
   * If true, we need to switch language models to the insert mode models (unrestricted English)
   * </pre>
   *
   * <code>bool insertMode = 2;</code>
   */
  public boolean getInsertMode() {
    return insertMode_;
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (enable_ != false) {
      output.writeBool(1, enable_);
    }
    if (insertMode_ != false) {
      output.writeBool(2, insertMode_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (enable_ != false) {
      size += com.google.protobuf.CodedOutputStream
        .computeBoolSize(1, enable_);
    }
    if (insertMode_ != false) {
      size += com.google.protobuf.CodedOutputStream
        .computeBoolSize(2, insertMode_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof io.github.keenon.voicecode.Request)) {
      return super.equals(obj);
    }
    io.github.keenon.voicecode.Request other = (io.github.keenon.voicecode.Request) obj;

    boolean result = true;
    result = result && (getEnable()
        == other.getEnable());
    result = result && (getInsertMode()
        == other.getInsertMode());
    result = result && unknownFields.equals(other.unknownFields);
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + ENABLE_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
        getEnable());
    hash = (37 * hash) + INSERTMODE_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
        getInsertMode());
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.github.keenon.voicecode.Request parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.github.keenon.voicecode.Request parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.github.keenon.voicecode.Request parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.github.keenon.voicecode.Request parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static io.github.keenon.voicecode.Request parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static io.github.keenon.voicecode.Request parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.github.keenon.voicecode.Request prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code Request}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:Request)
      io.github.keenon.voicecode.RequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.github.keenon.voicecode.DeepSpeechProto.internal_static_Request_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.github.keenon.voicecode.DeepSpeechProto.internal_static_Request_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.github.keenon.voicecode.Request.class, io.github.keenon.voicecode.Request.Builder.class);
    }

    // Construct using io.github.keenon.voicecode.Request.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      enable_ = false;

      insertMode_ = false;

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return io.github.keenon.voicecode.DeepSpeechProto.internal_static_Request_descriptor;
    }

    public io.github.keenon.voicecode.Request getDefaultInstanceForType() {
      return io.github.keenon.voicecode.Request.getDefaultInstance();
    }

    public io.github.keenon.voicecode.Request build() {
      io.github.keenon.voicecode.Request result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public io.github.keenon.voicecode.Request buildPartial() {
      io.github.keenon.voicecode.Request result = new io.github.keenon.voicecode.Request(this);
      result.enable_ = enable_;
      result.insertMode_ = insertMode_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.github.keenon.voicecode.Request) {
        return mergeFrom((io.github.keenon.voicecode.Request)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.github.keenon.voicecode.Request other) {
      if (other == io.github.keenon.voicecode.Request.getDefaultInstance()) return this;
      if (other.getEnable() != false) {
        setEnable(other.getEnable());
      }
      if (other.getInsertMode() != false) {
        setInsertMode(other.getInsertMode());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      io.github.keenon.voicecode.Request parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.github.keenon.voicecode.Request) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private boolean enable_ ;
    /**
     * <pre>
     * If false, stop speech transcript for the moment
     * </pre>
     *
     * <code>bool enable = 1;</code>
     */
    public boolean getEnable() {
      return enable_;
    }
    /**
     * <pre>
     * If false, stop speech transcript for the moment
     * </pre>
     *
     * <code>bool enable = 1;</code>
     */
    public Builder setEnable(boolean value) {
      
      enable_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * If false, stop speech transcript for the moment
     * </pre>
     *
     * <code>bool enable = 1;</code>
     */
    public Builder clearEnable() {
      
      enable_ = false;
      onChanged();
      return this;
    }

    private boolean insertMode_ ;
    /**
     * <pre>
     * If true, we need to switch language models to the insert mode models (unrestricted English)
     * </pre>
     *
     * <code>bool insertMode = 2;</code>
     */
    public boolean getInsertMode() {
      return insertMode_;
    }
    /**
     * <pre>
     * If true, we need to switch language models to the insert mode models (unrestricted English)
     * </pre>
     *
     * <code>bool insertMode = 2;</code>
     */
    public Builder setInsertMode(boolean value) {
      
      insertMode_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * If true, we need to switch language models to the insert mode models (unrestricted English)
     * </pre>
     *
     * <code>bool insertMode = 2;</code>
     */
    public Builder clearInsertMode() {
      
      insertMode_ = false;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:Request)
  }

  // @@protoc_insertion_point(class_scope:Request)
  private static final io.github.keenon.voicecode.Request DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.github.keenon.voicecode.Request();
  }

  public static io.github.keenon.voicecode.Request getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Request>
      PARSER = new com.google.protobuf.AbstractParser<Request>() {
    public Request parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Request(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<Request> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Request> getParserForType() {
    return PARSER;
  }

  public io.github.keenon.voicecode.Request getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

