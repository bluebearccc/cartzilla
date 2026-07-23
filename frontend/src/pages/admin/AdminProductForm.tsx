import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { adminProductApi, type CreateProductPayload, type ImagePayload, type VariantPayload } from '@/services/admin';
import { catalogApi } from '@/services/catalog';
import { Card } from '@/components/ui/Card';
import { Input, Select, Textarea } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { InlineMessage } from '@/components/ui/States';
import { useToast } from '@/components/ui/Toast';
import { formatVnd } from '@/lib/format';
import { ApiError } from '@/types/api';

function slugify(name: string) {
  return name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
}

const emptyVariant = (): VariantPayload => ({ sku: '', size: '', color: '', colorHex: '#000000', price: 0, stock: 0 });

export function AdminProductForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();

  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: catalogApi.getCategories });
  const { data: vendors } = useQuery({ queryKey: ['vendors'], queryFn: catalogApi.getVendors });
  const { data: existing } = useQuery({
    queryKey: ['admin-product', id],
    queryFn: () => adminProductApi.get(id!),
    enabled: isEdit,
  });

  const flattenedCategories = useMemo(() => {
    if (!categories) return [];
    const list: { id: string; label: string }[] = [];
    const walk = (items: typeof categories, prefix = '') => {
      for (const c of items) {
        list.push({ id: c.id, label: prefix ? `${prefix} ${c.name}` : c.name });
        if (c.children?.length) {
          walk(c.children, prefix ? `${prefix}──` : '└──');
        }
      }
    };
    walk(categories);
    return list;
  }, [categories]);

  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [categoryId, setCategoryId] = useState('');
  const [vendorId, setVendorId] = useState('');
  const [description, setDescription] = useState('');
  const [basePrice, setBasePrice] = useState(0);
  const [variants, setVariants] = useState<VariantPayload[]>([emptyVariant()]);
  const [images, setImages] = useState<ImagePayload[]>([]);
  const [imageUrl, setImageUrl] = useState('');
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (existing) {
      setName(existing.name);
      setSlug(existing.slug);
      setCategoryId(existing.categoryId ?? '');
      setVendorId(existing.vendorId ?? '');
      setDescription(existing.description ?? '');
      setBasePrice(existing.basePrice);
      setVariants(existing.variants.map((v) => ({ sku: v.sku, size: v.size ?? '', color: v.color ?? '', colorHex: v.colorHex ?? '#000000', price: v.price, stock: v.stock })));
      setImages(existing.images.map((im) => ({ id: im.id, imageUrl: im.imageUrl, altText: im.altText ?? '', isPrimary: im.isPrimary, sortOrder: im.sortOrder })));
    }
  }, [existing]);

  useEffect(() => {
    if (!slugTouched && !isEdit) setSlug(slugify(name));
  }, [name, slugTouched, isEdit]);

  const blockers: string[] = [];
  if (!name.trim()) blockers.push('Cần nhập tên sản phẩm');
  if (!categoryId) blockers.push('Cần chọn danh mục');
  if (!variants.length || variants.some((v) => !v.sku.trim())) blockers.push('Cần ít nhất 1 biến thể có SKU');
  if (!images.length) blockers.push('Cần ít nhất 1 ảnh');
  if (images.length && !images.some((im) => im.isPrimary)) blockers.push('Cần chọn 1 ảnh chính');

  const updateVariant = (i: number, patch: Partial<VariantPayload>) =>
    setVariants((vs) => vs.map((v, idx) => (idx === i ? { ...v, ...patch } : v)));

  const addImage = () => {
    if (!imageUrl.trim()) return;
    setImages((ims) => [...ims, { imageUrl: imageUrl.trim(), altText: name, isPrimary: ims.length === 0, sortOrder: ims.length }]);
    setImageUrl('');
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const res = await adminProductApi.uploadImage(file);
        setImages((ims) => [
          ...ims,
          { imageUrl: res.url, altText: name || file.name, isPrimary: ims.length === 0, sortOrder: ims.length },
        ]);
      }
      toast.success('Đã tải ảnh lên Cloudinary');
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : 'Upload ảnh thất bại');
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };
  const setPrimary = (i: number) => setImages((ims) => ims.map((im, idx) => ({ ...im, isPrimary: idx === i })));
  const removeImage = (i: number) => setImages((ims) => ims.filter((_, idx) => idx !== i).map((im, idx) => ({ ...im, sortOrder: idx })));

  const submit = async () => {
    if (blockers.length) {
      toast.error('Vui lòng hoàn tất các điều kiện xuất bản');
      return;
    }
    setSaving(true);
    const payload: CreateProductPayload = {
      categoryId,
      vendorId: vendorId || null,
      name,
      slug: slug || undefined,
      description,
      basePrice: basePrice || variants[0]?.price || 0,
      variants,
      images,
    };
    try {
      if (isEdit) {
        await adminProductApi.update(id!, {
          categoryId, vendorId: vendorId || null, name, slug, description,
          basePrice: payload.basePrice, active: true,
        });

        // Sync deleted images
        if (existing?.images) {
          const currentIds = new Set(images.map((im) => im.id).filter(Boolean));
          for (const origImg of existing.images) {
            if (!currentIds.has(origImg.id)) {
              await adminProductApi.deleteImage(id!, origImg.id);
            }
          }
        }

        // Add new images or update primary status
        for (const img of images) {
          if (!img.id) {
            await adminProductApi.addImage(id!, {
              imageUrl: img.imageUrl,
              altText: img.altText,
              isPrimary: img.isPrimary,
              sortOrder: img.sortOrder,
            });
          } else if (img.isPrimary) {
            const orig = existing?.images.find((o) => o.id === img.id);
            if (orig && !orig.isPrimary) {
              await adminProductApi.setPrimaryImage(id!, img.id);
            }
          }
        }

        await qc.invalidateQueries({ queryKey: ['admin-product', id] });
        await qc.invalidateQueries({ queryKey: ['admin-products'] });
        await qc.invalidateQueries({ queryKey: ['products'] });
        toast.success('Đã cập nhật sản phẩm');
      } else {
        await adminProductApi.create(payload);
        await qc.invalidateQueries({ queryKey: ['admin-products'] });
        await qc.invalidateQueries({ queryKey: ['products'] });
        toast.success('Đã tạo sản phẩm');
      }
      navigate('/admin/products');
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-5">
      <nav className="flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/admin/products" className="hover:text-brand">Sản phẩm</Link>
        <Icon name="chevron_right" className="text-[16px]" />
        <span className="text-ink">{isEdit ? 'Chỉnh sửa' : 'Thêm mới'}</span>
      </nav>
      <h1 className="font-headline text-2xl font-bold text-ink">{isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm'}</h1>

      {blockers.length > 0 && (
        <InlineMessage tone="warning">
          <span className="font-medium">Điều kiện xuất bản:</span> {blockers.join(' · ')}
        </InlineMessage>
      )}

      <div className="grid gap-5 lg:grid-cols-[1fr_320px]">
        <div className="space-y-5">
          <Card title="Thông tin cơ bản">
            <div className="space-y-4">
              <Input label="Tên sản phẩm" required value={name} onChange={(e) => setName(e.target.value)} />
              <Input label="Slug" value={slug} onChange={(e) => { setSlug(e.target.value); setSlugTouched(true); }} helper="Tự sinh từ tên, có thể sửa" />
              <div className="grid grid-cols-2 gap-3">
                <Select label="Danh mục" required value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                  <option value="">— Chọn danh mục —</option>
                  {flattenedCategories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.label}
                    </option>
                  ))}
                </Select>
                <Select label="Thương hiệu (tùy chọn)" value={vendorId} onChange={(e) => setVendorId(e.target.value)}>
                  <option value="">— Không —</option>
                  {(vendors ?? []).filter((v) => v.active).map((v) => <option key={v.id} value={v.id}>{v.name}</option>)}
                </Select>
              </div>
              <Input label="Giá cơ bản (₫)" type="number" value={basePrice} onChange={(e) => setBasePrice(Number(e.target.value))} />
              <Textarea label="Mô tả" rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
          </Card>

          <Card title="Biến thể (Variants)" action={<Button size="sm" variant="secondary" leadingIcon="add" onClick={() => setVariants((v) => [...v, emptyVariant()])}>Thêm biến thể</Button>}>
            <div className="space-y-3">
              <div className="hidden grid-cols-[1.5fr_0.8fr_1fr_0.6fr_1fr_0.8fr_36px] gap-2 text-xs font-semibold uppercase text-ink-muted md:grid">
                <span>SKU</span><span>Size</span><span>Màu</span><span>Hex</span><span>Giá</span><span>Tồn</span><span />
              </div>
              {variants.map((v, i) => (
                <div key={i} className="grid grid-cols-2 gap-2 md:grid-cols-[1.5fr_0.8fr_1fr_0.6fr_1fr_0.8fr_36px]">
                  <input className="h-10 rounded border border-border px-2 text-sm uppercase" placeholder="SKU" value={v.sku} onChange={(e) => updateVariant(i, { sku: e.target.value.toUpperCase() })} />
                  <select
                    className="h-10 rounded border border-border px-2 text-sm bg-white"
                    value={v.size}
                    onChange={(e) => updateVariant(i, { size: e.target.value })}
                  >
                    <option value="">-- Size --</option>
                    <optgroup label="Áo quần">
                      <option value="S">S</option>
                      <option value="M">M</option>
                      <option value="L">L</option>
                      <option value="XL">XL</option>
                      <option value="2XL">2XL</option>
                      <option value="3XL">3XL</option>
                      <option value="F">Free Size (F)</option>
                    </optgroup>
                    <optgroup label="Quần Jean / Short (Size số)">
                      <option value="28">28</option>
                      <option value="29">29</option>
                      <option value="30">30</option>
                      <option value="31">31</option>
                      <option value="32">32</option>
                      <option value="33">33</option>
                      <option value="34">34</option>
                      <option value="35">35</option>
                      <option value="36">36</option>
                    </optgroup>
                    <optgroup label="Giày dép">
                      <option value="36">36</option>
                      <option value="37">37</option>
                      <option value="38">38</option>
                      <option value="39">39</option>
                      <option value="40">40</option>
                      <option value="41">41</option>
                      <option value="42">42</option>
                      <option value="43">43</option>
                      <option value="44">44</option>
                    </optgroup>
                    {v.size && !['S','M','L','XL','2XL','3XL','F','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44'].includes(v.size) && (
                      <option value={v.size}>{v.size}</option>
                    )}
                  </select>
                  <input className="h-10 rounded border border-border px-2 text-sm" placeholder="Trắng" value={v.color} onChange={(e) => updateVariant(i, { color: e.target.value })} />
                  <input type="color" className="h-10 w-full rounded border border-border" value={v.colorHex} onChange={(e) => updateVariant(i, { colorHex: e.target.value })} />
                  <input type="number" className="h-10 rounded border border-border px-2 text-sm" placeholder="Giá" value={v.price} onChange={(e) => updateVariant(i, { price: Number(e.target.value) })} />
                  <input type="number" className="h-10 rounded border border-border px-2 text-sm" placeholder="Tồn" value={v.stock} onChange={(e) => updateVariant(i, { stock: Number(e.target.value) })} />
                  <button onClick={() => setVariants((vs) => vs.filter((_, idx) => idx !== i))} className="flex h-10 items-center justify-center text-ink-muted hover:text-danger">
                    <Icon name="delete" className="text-[18px]" />
                  </button>
                </div>
              ))}
            </div>
          </Card>

          <Card title="Hình ảnh">
            <div className="space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg bg-black px-3.5 py-2 text-sm font-medium text-white hover:bg-black/80 transition-colors shadow-sm">
                  <Icon name="upload" className="text-[18px]" />
                  {uploading ? 'Đang tải lên...' : 'Tải ảnh từ máy'}
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    disabled={uploading}
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>
                <span className="text-xs text-ink-muted">hoặc</span>
                <div className="flex flex-1 gap-2 min-w-[240px]">
                  <Input placeholder="Dán URL ảnh..." value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} />
                  <Button variant="secondary" onClick={addImage}>Thêm link</Button>
                </div>
              </div>
              <div className="mt-4 grid grid-cols-3 gap-3 sm:grid-cols-4">
                {images.map((im, i) => (
                  <div key={i} className={`relative overflow-hidden rounded-lg border-2 ${im.isPrimary ? 'border-brand' : 'border-border'}`}>
                    <img src={im.imageUrl} alt="" className="aspect-square w-full object-cover" />
                    <div className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-ink/60 px-1.5 py-1">
                      <label className="flex items-center gap-1 text-[11px] text-white">
                        <input type="radio" name="primary" checked={im.isPrimary} onChange={() => setPrimary(i)} /> Chính
                      </label>
                      <button onClick={() => removeImage(i)} className="text-white hover:text-danger"><Icon name="close" className="text-[16px]" /></button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </Card>
        </div>

        <div className="space-y-5">
          <Card title="Xem trước">
            <div className="overflow-hidden rounded-lg border border-border">
              <div className="aspect-square bg-page">
                {images.find((im) => im.isPrimary)?.imageUrl ? (
                  <img src={images.find((im) => im.isPrimary)!.imageUrl} alt="" className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full items-center justify-center text-ink-muted"><Icon name="image" className="text-[40px]" /></div>
                )}
              </div>
              <div className="p-3">
                <p className="line-clamp-2 text-sm font-medium text-ink">{name || 'Tên sản phẩm'}</p>
                <p className="mt-1 font-bold text-ink">{formatVnd(basePrice || variants[0]?.price || 0)}</p>
              </div>
            </div>
          </Card>
          <div className="sticky top-20 flex flex-col gap-2 rounded-lg border border-border bg-white p-4">
            <Button loading={saving} disabled={blockers.length > 0} onClick={submit}>Lưu & xuất bản</Button>
            <Button variant="secondary" onClick={() => navigate('/admin/products')}>Hủy</Button>
          </div>
        </div>
      </div>
    </div>
  );
}
